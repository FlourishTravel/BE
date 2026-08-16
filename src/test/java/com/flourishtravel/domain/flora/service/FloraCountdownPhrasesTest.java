package com.flourishtravel.domain.flora.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FloraCountdownPhrasesTest {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    @Test
    void overnightGatheringUsesClockNotRawMinutes() {
        Instant now = ZonedDateTime.of(2026, 8, 16, 23, 16, 0, 0, VN).toInstant();
        Instant meeting = ZonedDateTime.of(2026, 8, 17, 8, 0, 0, 0, VN).toInstant();
        long minutes = 523L;
        String phrase = FloraCountdownPhrases.meetingWhen(meeting, minutes, VN, now);
        assertEquals("lúc 08:00 ngày mai", phrase);
    }

    @Test
    void soonUsesMinutesAndClock() {
        Instant now = ZonedDateTime.of(2026, 8, 17, 7, 45, 0, 0, VN).toInstant();
        Instant meeting = ZonedDateTime.of(2026, 8, 17, 8, 0, 0, 0, VN).toInstant();
        String phrase = FloraCountdownPhrases.meetingWhen(meeting, 15L, VN, now);
        assertEquals("còn ~15 phút, lúc 08:00", phrase);
    }

    @Test
    void sameDayFarAwayUsesClockOnly() {
        Instant now = ZonedDateTime.of(2026, 8, 17, 8, 0, 0, 0, VN).toInstant();
        Instant meeting = ZonedDateTime.of(2026, 8, 17, 14, 30, 0, 0, VN).toInstant();
        String phrase = FloraCountdownPhrases.meetingWhen(meeting, 390L, VN, now);
        assertEquals("lúc 14:30", phrase);
    }

    @Test
    void noClockAndHugeMinutesIsOmitted() {
        assertNull(FloraCountdownPhrases.meetingWhen(null, 523L, VN));
    }
}
