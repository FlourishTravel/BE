package com.flourishtravel.domain.booking.service;

import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.payment.entity.Refund;
import com.flourishtravel.domain.tour.entity.TourSession;
import com.flourishtravel.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingRefundEligibilityTest {

    private BookingRefundEligibility eligibility;

    @BeforeEach
    void setUp() {
        eligibility = new BookingRefundEligibility();
        ReflectionTestUtils.setField(eligibility, "tourTimezone", "Asia/Ho_Chi_Minh");
    }

    @Test
    void paidBeforeStart_canRequestRefund() {
        Booking booking = booking("paid", LocalDate.now().plusDays(3), null);
        assertTrue(eligibility.canRequestRefund(booking));
    }

    @Test
    void paidOnStartDay_cannotRequestRefund() {
        Booking booking = booking("paid", LocalDate.now(), null);
        assertFalse(eligibility.canRequestRefund(booking));
        assertNotNull(eligibility.refusalReason(booking));
    }

    @Test
    void confirmed_cannotRequestRefund() {
        Booking booking = booking("confirmed", LocalDate.now().plusDays(5), null);
        assertFalse(eligibility.canRequestRefund(booking));
    }

    @Test
    void pendingRefund_cannotRequestAgain() {
        Refund pending = Refund.builder().status("pending").build();
        Booking booking = booking("paid", LocalDate.now().plusDays(5), List.of(pending));
        assertFalse(eligibility.canRequestRefund(booking));
    }

    private static Booking booking(String status, LocalDate start, List<Refund> refunds) {
        TourSession session = TourSession.builder().startDate(start).endDate(start).build();
        return Booking.builder()
                .user(User.builder().build())
                .session(session)
                .status(status)
                .refunds(refunds)
                .build();
    }
}
