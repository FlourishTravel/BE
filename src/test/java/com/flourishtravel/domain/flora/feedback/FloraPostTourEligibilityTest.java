package com.flourishtravel.domain.flora.feedback;

import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.payment.entity.Refund;
import com.flourishtravel.domain.tour.entity.TourSession;
import com.flourishtravel.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FloraPostTourEligibilityTest {

    private FloraPostTourEligibility eligibility;
    private Booking booking;
    private TourSession session;

    @BeforeEach
    void setUp() throws Exception {
        eligibility = new FloraPostTourEligibility();
        Field tz = FloraPostTourEligibility.class.getDeclaredField("tourTimezone");
        tz.setAccessible(true);
        tz.set(eligibility, "Asia/Ho_Chi_Minh");

        session = TourSession.builder()
                .startDate(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).minusDays(5))
                .endDate(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).minusDays(1))
                .build();
        booking = Booking.builder()
                .user(User.builder().build())
                .session(session)
                .status("completed")
                .build();
        booking.setId(UUID.randomUUID());
    }

    @Test
    void completedBookingWithEndedSession_isEligible() {
        assertTrue(eligibility.isEligible(booking));
    }

    @Test
    void upcomingBooking_isNotEligible() {
        session.setEndDate(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(3));
        booking.setStatus("confirmed");
        assertFalse(eligibility.isEligible(booking));
    }

    @Test
    void cancelledBooking_isNotEligible() {
        booking.setStatus("cancelled");
        assertFalse(eligibility.isEligible(booking));
    }

    @Test
    void approvedRefund_invalidatesEligibility() {
        Refund refund = Refund.builder().status("approved").build();
        booking.setRefunds(List.of(refund));
        assertTrue(eligibility.hasInvalidatingRefund(booking));
        assertFalse(eligibility.isEligible(booking));
    }

    @Test
    void pendingRefund_doesNotInvalidate() {
        booking.setRefunds(List.of(Refund.builder().status("pending").build()));
        assertFalse(eligibility.hasInvalidatingRefund(booking));
        assertTrue(eligibility.isEligible(booking));
    }

    @Test
    void paidWithEndedSession_isEligible() {
        booking.setStatus("paid");
        assertTrue(eligibility.isEligible(booking));
    }
}
