package com.flourishtravel.domain.booking.service;

import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.booking.entity.BookingGuest;
import com.flourishtravel.domain.booking.entity.SessionParticipant;
import com.flourishtravel.domain.booking.repository.BookingRepository;
import com.flourishtravel.domain.booking.repository.SessionParticipantActivityAttendanceRepository;
import com.flourishtravel.domain.booking.repository.SessionParticipantRepository;
import com.flourishtravel.domain.tour.entity.TourSession;
import com.flourishtravel.domain.user.entity.User;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Đồng bộ danh sách người tham gia trên từng session từ booking đã chốt khách (paid / confirmed / completed) + booking_guests.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionParticipantSyncService {

    public static final String ROSTER_LEAD = "LEAD";
    public static final String ROLE_LEAD = "LEAD";
    public static final String ROLE_COMPANION = "COMPANION";

    /** Tạm thời đẩy line_index của companion ra khỏi dải 0..n để tránh vi phạm uk_session_booking_line khi đổi thứ tự. */
    private static final int LINE_INDEX_STAGING_BASE = 1_000_000;

    private final SessionParticipantRepository participantRepository;
    private final SessionParticipantActivityAttendanceRepository activityAttendanceRepository;
    private final BookingRepository bookingRepository;
    private final EntityManager entityManager;

    @Transactional
    public void syncPaidBooking(UUID bookingId) {
        bookingRepository.findByIdWithGuests(bookingId).ifPresent(this::syncPaidBooking);
    }

    @Transactional
    public void syncPaidBooking(Booking b) {
        if (b == null || b.getSession() == null || b.getUser() == null) {
            return;
        }
        String st = b.getStatus() == null ? "" : b.getStatus().toLowerCase(Locale.ROOT);
        if (!isRosterEligibleStatus(st)) {
            return;
        }

        TourSession session = b.getSession();
        User lead = b.getUser();

        Set<String> keepKeys = new HashSet<>();
        keepKeys.add(ROSTER_LEAD);

        upsertLead(session, b, lead);

        stageCompanionLineIndicesAwayFromDisplayRange(b);

        List<BookingGuest> companions = b.getBookingGuests() == null ? List.of() : b.getBookingGuests().stream()
                .sorted(Comparator.comparing(BookingGuest::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        int idx = 1;
        for (BookingGuest g : companions) {
            if (isBookingGuestDuplicateOfLead(g, lead)) {
                continue;
            }
            keepKeys.add(g.getId().toString());
            upsertCompanion(session, b, g, idx++);
        }

        List<SessionParticipant> existing = participantRepository.findByBooking_Id(b.getId());
        for (SessionParticipant row : existing) {
            if (!keepKeys.contains(row.getRosterKey())) {
                activityAttendanceRepository.findBySessionParticipant_Id(row.getId())
                        .forEach(activityAttendanceRepository::delete);
                participantRepository.delete(row);
            }
        }
    }

    @Transactional
    public void syncAllPaidForSession(TourSession session) {
        List<Booking> roster = bookingRepository.findBySessionAndRosterStatusesWithGuests(session);
        for (Booking b : roster) {
            syncPaidBooking(b);
        }
    }

    /** Trạng thái booking được coi là đã có trên đoàn (đồng bộ roster). */
    public static boolean isRosterEligibleStatus(String normalizedLowerStatus) {
        if (normalizedLowerStatus == null || normalizedLowerStatus.isEmpty()) {
            return false;
        }
        return "paid".equals(normalizedLowerStatus)
                || "confirmed".equals(normalizedLowerStatus)
                || "completed".equals(normalizedLowerStatus);
    }

    private void upsertLead(TourSession session, Booking b, User lead) {
        SessionParticipant row = participantRepository
                .findBySession_IdAndBooking_IdAndRosterKey(session.getId(), b.getId(), ROSTER_LEAD)
                .orElseGet(() -> SessionParticipant.builder()
                        .session(session)
                        .booking(b)
                        .rosterKey(ROSTER_LEAD)
                        .lineIndex(0)
                        .participantRole(ROLE_LEAD)
                        .build());

        row.setLineIndex(0);
        row.setUser(lead);
        row.setBookingGuest(null);
        row.setDisplayName(lead.getFullName() != null && !lead.getFullName().isBlank()
                ? lead.getFullName()
                : lead.getEmail());
        row.setPhoneSnapshot(firstNonBlank(b.getContactPhone(), lead.getPhone()));
        row.setParticipantRole(ROLE_LEAD);
        participantRepository.save(row);
    }

    /**
     * Khi sort_order của booking_guests đổi, gán line_index mới lần lượt (1,2,…) có thể trùng với dòng chưa cập nhật
     * (cùng session + booking). Đưa mọi companion sang chỉ số tạm, flush, rồi upsertCompanion mới gán 1..n an toàn.
     */
    private void stageCompanionLineIndicesAwayFromDisplayRange(Booking b) {
        List<SessionParticipant> rows = participantRepository.findByBooking_Id(b.getId());
        int slot = 0;
        for (SessionParticipant row : rows) {
            if (ROSTER_LEAD.equals(row.getRosterKey())) {
                continue;
            }
            row.setLineIndex(LINE_INDEX_STAGING_BASE + slot++);
            participantRepository.save(row);
        }
        entityManager.flush();
    }

    private void upsertCompanion(TourSession session, Booking b, BookingGuest g, int lineIndex) {
        String key = g.getId().toString();
        SessionParticipant row = participantRepository
                .findBySession_IdAndBooking_IdAndRosterKey(session.getId(), b.getId(), key)
                .orElseGet(() -> SessionParticipant.builder()
                        .session(session)
                        .booking(b)
                        .rosterKey(key)
                        .participantRole(ROLE_COMPANION)
                        .build());

        row.setLineIndex(lineIndex);
        row.setBookingGuest(g);
        row.setUser(null);
        row.setDisplayName(g.getFullName());
        row.setPhoneSnapshot(null);
        row.setParticipantRole(ROLE_COMPANION);
        participantRepository.save(row);
    }

    private String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a.trim();
        }
        if (b != null && !b.isBlank()) {
            return b.trim();
        }
        return null;
    }

    /**
     * Một số form/seed lưu thêm 1 dòng booking_guest trùng thông tin người đặt — không tạo thêm slot roster.
     */
    public static boolean isBookingGuestDuplicateOfLead(BookingGuest g, User lead) {
        if (g == null || lead == null) {
            return false;
        }
        String gn = g.getFullName() == null ? "" : g.getFullName().trim().toLowerCase(Locale.ROOT);
        String ln = lead.getFullName() == null ? "" : lead.getFullName().trim().toLowerCase(Locale.ROOT);
        if (gn.isEmpty() || ln.isEmpty() || !gn.equals(ln)) {
            return false;
        }
        if (g.getDateOfBirth() != null && lead.getDateOfBirth() != null) {
            return g.getDateOfBirth().equals(lead.getDateOfBirth());
        }
        return true;
    }
}
