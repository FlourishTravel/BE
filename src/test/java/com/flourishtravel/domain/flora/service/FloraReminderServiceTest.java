package com.flourishtravel.domain.flora.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FloraReminderServiceTest {

    @Test
    void meetingReminderKey_isStableForSameMeetingInstant() {
        UUID bookingId = UUID.randomUUID();
        Instant t = Instant.parse("2026-06-25T08:00:30Z");
        String k1 = FloraReminderService.meetingReminderKey(bookingId, "TOUR_REMINDER_15_MINUTES", t);
        String k2 = FloraReminderService.meetingReminderKey(bookingId, "TOUR_REMINDER_15_MINUTES", t);
        assertEquals(k1, k2);
    }

    @Test
    void meetingReminderKey_differsByType() {
        UUID bookingId = UUID.randomUUID();
        Instant t = Instant.now();
        String k1 = FloraReminderService.meetingReminderKey(bookingId, "A", t);
        String k2 = FloraReminderService.meetingReminderKey(bookingId, "B", t);
        assertNotEquals(k1, k2);
    }

    @Test
    void meetingReminderKey_differsWhenMeetingTimeChanges() {
        UUID bookingId = UUID.randomUUID();
        Instant t1 = Instant.parse("2026-06-25T03:40:00Z");
        Instant t2 = Instant.parse("2026-06-25T04:00:00Z");
        assertNotEquals(
                FloraReminderService.meetingReminderKey(bookingId, "TOUR_REMINDER_15_MINUTES", t1),
                FloraReminderService.meetingReminderKey(bookingId, "TOUR_REMINDER_15_MINUTES", t2));
    }
}
