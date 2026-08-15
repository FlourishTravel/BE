package com.flourishtravel.domain.payment.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentHoldRulesTest {

    @Test
    void holdIsAtLeastFiveMinutes() {
        assertEquals(300, PaymentHoldRules.holdSeconds(60));
        assertEquals(900, PaymentHoldRules.holdSeconds(900));
    }

    @Test
    void unpaidCheckoutExpiresAfterHold() {
        Instant created = Instant.parse("2026-08-15T10:00:00Z");
        assertFalse(PaymentHoldRules.isPastHold(created, Instant.parse("2026-08-15T10:14:59Z"), 900));
        assertTrue(PaymentHoldRules.isPastHold(created, Instant.parse("2026-08-15T10:15:00Z"), 900));
    }
}
