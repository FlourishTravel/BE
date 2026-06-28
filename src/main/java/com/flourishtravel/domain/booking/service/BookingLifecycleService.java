package com.flourishtravel.domain.booking.service;

import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.booking.repository.BookingRepository;
import com.flourishtravel.domain.tour.entity.TourSession;
import com.flourishtravel.domain.tour.repository.TourSessionRepository;
import com.flourishtravel.domain.tour.service.TourSessionStatusResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Tự cập nhật trạng thái booking/session theo lịch khởi hành / kết thúc.
 * <p>
 * Session: scheduled → ongoing (trong khoảng start–end) → completed (sau endDate).
 * {@code confirmed} booking vẫn do admin xác nhận thủ công; job đóng booking sau ngày kết thúc.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingLifecycleService {

    private static final Set<String> COMPLETABLE_BOOKING_STATUSES = Set.of("paid", "confirmed");

    private final BookingRepository bookingRepository;
    private final TourSessionRepository sessionRepository;

    @Value("${app.flora.timezone:Asia/Ho_Chi_Minh}")
    private String tourTimezone;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void syncOnStartup() {
        syncSessionAndBookingLifecycle();
    }

    @Scheduled(cron = "${app.booking.lifecycle-cron:0 5 0 * * *}", zone = "${app.flora.timezone:Asia/Ho_Chi_Minh}")
    @Transactional
    public void closeEndedTrips() {
        syncSessionAndBookingLifecycle();
    }

    @Transactional
    public void syncSessionAndBookingLifecycle() {
        LocalDate today = TourSessionStatusResolver.todayInZone(tourTimezone);

        List<TourSession> endedSessions = sessionRepository.findActiveSessionsEndedBefore(today);
        for (TourSession session : endedSessions) {
            session.setStatus(TourSessionStatusResolver.COMPLETED);
            sessionRepository.save(session);
            log.info("[BookingLifecycle] session {} -> completed (ended {})", session.getId(), session.getEndDate());
        }

        List<TourSession> inProgress = sessionRepository.findScheduledSessionsInProgress(today);
        for (TourSession session : inProgress) {
            session.setStatus(TourSessionStatusResolver.ONGOING);
            sessionRepository.save(session);
            log.info("[BookingLifecycle] session {} -> ongoing ({} – {})", session.getId(), session.getStartDate(), session.getEndDate());
        }

        List<TourSession> revertScheduled = sessionRepository.findOngoingSessionsBeforeStart(today);
        for (TourSession session : revertScheduled) {
            session.setStatus(TourSessionStatusResolver.SCHEDULED);
            sessionRepository.save(session);
            log.info("[BookingLifecycle] session {} -> scheduled (before start)", session.getId());
        }

        LocalDate bookingCutoff = today.minusDays(1);
        List<Booking> ended = bookingRepository.findEndedBookingsWithStatuses(bookingCutoff, COMPLETABLE_BOOKING_STATUSES);
        for (Booking booking : ended) {
            booking.setStatus("completed");
            bookingRepository.save(booking);
            log.info("[BookingLifecycle] booking {} -> completed (session ended)", booking.getId());
        }
    }
}
