package com.flourishtravel.domain.flora.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Cách nói giờ tập trung cho Flora: số phút thô chỉ khi sắp tới.
 * 523 phút lúc 23h → "lúc 08:00 ngày mai", không "còn 523 phút".
 */
public final class FloraCountdownPhrases {

    static final int RAW_MINUTES_MAX = 90;

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM");

    private FloraCountdownPhrases() {}

    public static String meetingWhen(Instant meetingAt, Long minutesUntil, ZoneId zone) {
        return meetingWhen(meetingAt, minutesUntil, zone, Instant.now());
    }

    public static String meetingWhen(Instant meetingAt, Long minutesUntil, ZoneId zone, Instant now) {
        if (zone == null) {
            zone = ZoneId.of("Asia/Ho_Chi_Minh");
        }
        if (meetingAt != null) {
            ZonedDateTime at = meetingAt.atZone(zone);
            LocalDate today = now.atZone(zone).toLocalDate();
            LocalDate meetDay = at.toLocalDate();
            String clock = at.toLocalTime().format(CLOCK);
            if (meetDay.equals(today)) {
                if (minutesUntil != null && minutesUntil >= 0 && minutesUntil <= RAW_MINUTES_MAX) {
                    return "còn ~" + minutesUntil + " phút, lúc " + clock;
                }
                return "lúc " + clock;
            }
            if (meetDay.equals(today.plusDays(1))) {
                return "lúc " + clock + " ngày mai";
            }
            return "lúc " + clock + " ngày " + at.format(DAY);
        }
        if (minutesUntil != null && minutesUntil >= 0 && minutesUntil <= RAW_MINUTES_MAX) {
            return "còn ~" + minutesUntil + " phút";
        }
        return null;
    }
}
