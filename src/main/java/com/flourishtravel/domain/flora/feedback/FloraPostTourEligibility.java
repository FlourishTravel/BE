package com.flourishtravel.domain.flora.feedback;

import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.payment.entity.Refund;
import com.flourishtravel.domain.tour.entity.TourSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Set;

@Component
public class FloraPostTourEligibility {

    private static final Set<String> ENDED_ELIGIBLE_STATUSES = Set.of("paid", "confirmed", "completed");

    @Value("${app.flora.timezone:Asia/Ho_Chi_Minh}")
    private String tourTimezone;

    public boolean isEligible(Booking booking) {
        if (booking == null || booking.getStatus() == null) return false;
        String status = booking.getStatus().toLowerCase(Locale.ROOT);
        if ("cancelled".equals(status)) return false;
        if (hasInvalidatingRefund(booking)) return false;
        if (!ENDED_ELIGIBLE_STATUSES.contains(status)) return false;
        return isSessionEnded(booking);
    }

    public boolean hasInvalidatingRefund(Booking booking) {
        if (booking.getRefunds() == null || booking.getRefunds().isEmpty()) return false;
        return booking.getRefunds().stream()
                .map(Refund::getStatus)
                .filter(s -> s != null)
                .anyMatch(s -> {
                    String normalized = s.toLowerCase(Locale.ROOT);
                    return "completed".equals(normalized) || "approved".equals(normalized);
                });
    }

    public boolean isSessionEnded(Booking booking) {
        TourSession session = booking.getSession();
        if (session == null || session.getEndDate() == null) return false;
        LocalDate today = LocalDate.now(ZoneId.of(tourTimezone));
        return today.isAfter(session.getEndDate());
    }

    public ZonedDateTime resolveCompletedAt(Booking booking) {
        TourSession session = booking.getSession();
        if (session == null || session.getEndDate() == null) return null;
        ZoneId zone = ZoneId.of(tourTimezone);
        return session.getEndDate().atTime(18, 0).atZone(zone);
    }
}
