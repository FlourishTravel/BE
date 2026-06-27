package com.flourishtravel.domain.booking.service;

import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.booking.repository.BookingRepository;
import com.flourishtravel.domain.tour.entity.TourSession;
import com.flourishtravel.domain.tour.repository.TourSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Tự cập nhật trạng thái booking/session theo lịch khởi hành.
 * <p>
 * {@code confirmed} vẫn do admin xác nhận thủ công; job này chỉ đóng chuyến sau ngày kết thúc.
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

    @Scheduled(cron = "${app.booking.lifecycle-cron:0 5 0 * * *}", zone = "${app.flora.timezone:Asia/Ho_Chi_Minh}")
    @Transactional
    public void closeEndedTrips() {
        LocalDate today = LocalDate.now(ZoneId.of(tourTimezone));
        LocalDate cutoff = today.minusDays(1);

        List<Booking> ended = bookingRepository.findEndedBookingsWithStatuses(cutoff, COMPLETABLE_BOOKING_STATUSES);
        for (Booking booking : ended) {
            booking.setStatus("completed");
            bookingRepository.save(booking);
            log.info("[BookingLifecycle] booking {} -> completed (session ended)", booking.getId());
        }

        List<TourSession> sessions = sessionRepository.findScheduledSessionsEndedBefore(cutoff);
        for (TourSession session : sessions) {
            session.setStatus("completed");
            sessionRepository.save(session);
            log.info("[BookingLifecycle] session {} -> completed", session.getId());
        }
    }
}
