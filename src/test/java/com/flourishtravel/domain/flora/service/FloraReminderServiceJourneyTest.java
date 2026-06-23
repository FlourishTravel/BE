package com.flourishtravel.domain.flora.service;

import com.flourishtravel.domain.flora.FloraReminderTypes;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FloraReminderServiceJourneyTest {

    @Test
    void meetingReminderKey_changesWhenMeetingTimeChanges() {
        UUID bookingId = UUID.randomUUID();
        Instant t1 = Instant.parse("2026-06-25T03:40:00Z");
        Instant t2 = Instant.parse("2026-06-25T04:00:00Z");
        String k1 = FloraReminderService.meetingReminderKey(bookingId, FloraReminderTypes.TOUR_REMINDER_15_MINUTES, t1);
        String k2 = FloraReminderService.meetingReminderKey(bookingId, FloraReminderTypes.TOUR_REMINDER_15_MINUTES, t2);
        assertNotEquals(k1, k2);
    }

    @Test
    void idempotencyKey_delegatesToMeetingReminderKey() {
        UUID bookingId = UUID.randomUUID();
        Instant t = Instant.parse("2026-06-25T03:40:00Z");
        assertEquals(
                FloraReminderService.meetingReminderKey(bookingId, "X", t),
                FloraReminderService.idempotencyKey(bookingId, "X", t));
    }
}
