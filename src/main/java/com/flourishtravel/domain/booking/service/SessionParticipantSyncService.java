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

import java.util.ArrayList;
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
        RosterCompanionPlan plan = selectCompanionsForRoster(lead, b.getBookingGuests(), b.getGuestCount());

        Set<String> keepKeys = new HashSet<>();
        keepKeys.add(ROSTER_LEAD);

        upsertLead(session, b, lead, plan.skippedFormLead());

        stageCompanionLineIndicesAwayFromDisplayRange(b);

        int idx = 1;
        for (BookingGuest g : plan.companions()) {
            keepKeys.add(g.getId().toString());
            upsertCompanion(session, b, g, idx++);
        }

        List<SessionParticipant> existing = participantRepository.findByBooking_Id(b.getId());
        SessionParticipant leadRow = existing.stream()
                .filter(row -> ROSTER_LEAD.equals(row.getRosterKey()))
                .findFirst()
                .orElse(null);
        boolean leadTouched = false;
        for (SessionParticipant row : existing) {
            if (keepKeys.contains(row.getRosterKey())) {
                continue;
            }
            // Dòng thừa thường là người đặt bị nhân đôi — giữ mốc điểm danh trên LEAD nếu có.
            if (leadRow != null) {
                if (leadRow.getCheckInAt() == null && row.getCheckInAt() != null) {
                    leadRow.setCheckInAt(row.getCheckInAt());
                    leadTouched = true;
                }
                if (leadRow.getCheckOutAt() == null && row.getCheckOutAt() != null) {
                    leadRow.setCheckOutAt(row.getCheckOutAt());
                    leadTouched = true;
                }
            }
            activityAttendanceRepository.findBySessionParticipant_Id(row.getId())
                    .forEach(activityAttendanceRepository::delete);
            participantRepository.delete(row);
        }
        if (leadTouched) {
            participantRepository.save(leadRow);
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

    private void upsertLead(TourSession session, Booking b, User lead, BookingGuest formLead) {
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
        String accountName = lead.getFullName() != null && !lead.getFullName().isBlank()
                ? lead.getFullName().trim()
                : lead.getEmail();
        String formName = formLead != null && formLead.getFullName() != null && !formLead.getFullName().isBlank()
                ? formLead.getFullName().trim()
                : null;
        row.setDisplayName(formName != null ? formName : accountName);
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
     * Checkout/app gửi cả người đặt trong {@code booking_guests}; seed chỉ gửi khách kèm.
     * Roster luôn có 1 LEAD từ tài khoản — companion tối đa {@code guestCount - 1}.
     */
    public static RosterCompanionPlan selectCompanionsForRoster(
            User lead, List<BookingGuest> guests, Integer guestCount) {
        int seats = effectiveGuestCount(guestCount);
        int maxCompanions = Math.max(0, seats - 1);
        List<BookingGuest> sorted = guests == null ? List.of() : guests.stream()
                .sorted(Comparator.comparing(BookingGuest::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        boolean skippedLead = false;
        BookingGuest skippedFormLead = null;
        List<BookingGuest> out = new ArrayList<>();
        for (BookingGuest g : sorted) {
            if (!skippedLead && isBookingGuestDuplicateOfLead(g, lead)) {
                skippedLead = true;
                skippedFormLead = g;
                continue;
            }
            out.add(g);
        }
        // Tên trên form không khớp tài khoản → dòng đầu là người đặt, không phải khách kèm.
        if (!skippedLead && out.size() > maxCompanions) {
            skippedFormLead = out.remove(0);
        }
        if (out.size() > maxCompanions) {
            out = new ArrayList<>(out.subList(0, maxCompanions));
        }
        return new RosterCompanionPlan(List.copyOf(out), skippedFormLead);
    }

    public static int effectiveGuestCount(Integer guestCount) {
        if (guestCount == null || guestCount < 1) {
            return 1;
        }
        return guestCount;
    }

    /**
     * Một số form/seed lưu thêm 1 dòng booking_guest trùng thông tin người đặt — không tạo thêm slot roster.
     */
    public static boolean isBookingGuestDuplicateOfLead(BookingGuest g, User lead) {
        if (g == null || lead == null) {
            return false;
        }
        String gn = normalizePersonName(g.getFullName());
        String ln = normalizePersonName(lead.getFullName());
        if (gn.isEmpty() || ln.isEmpty() || !gn.equals(ln)) {
            return false;
        }
        if (g.getDateOfBirth() != null && lead.getDateOfBirth() != null) {
            return g.getDateOfBirth().equals(lead.getDateOfBirth());
        }
        return true;
    }

    static String normalizePersonName(String name) {
        if (name == null) {
            return "";
        }
        return name.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    /** Kết quả tách khách kèm khỏi người đặt trên form. */
    public record RosterCompanionPlan(List<BookingGuest> companions, BookingGuest skippedFormLead) {
        public RosterCompanionPlan {
            companions = companions == null ? List.of() : List.copyOf(companions);
        }
    }
}
