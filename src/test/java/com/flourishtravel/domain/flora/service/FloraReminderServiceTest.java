package com.flourishtravel.domain.flora.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FloraReminderServiceTest {

    @Test
    void idempotencyKey_isStableForSameMinute() {
        UUID bookingId = UUID.randomUUID();
        Instant t = Instant.parse("2026-06-25T08:00:30Z");
        String k1 = FloraReminderService.idempotencyKey(bookingId, "TOUR_REMINDER_15_MINUTES", t);
        String k2 = FloraReminderService.idempotencyKey(bookingId, "TOUR_REMINDER_15_MINUTES", t.plusSeconds(10));
        assertEquals(k1, k2);
    }

    @Test
    void idempotencyKey_differsByType() {
        UUID bookingId = UUID.randomUUID();
        Instant t = Instant.now();
        String k1 = FloraReminderService.idempotencyKey(bookingId, "A", t);
        String k2 = FloraReminderService.idempotencyKey(bookingId, "B", t);
        assertNotEquals(k1, k2);
    }
}
