package com.flourishtravel.domain.notification.push;

import com.flourishtravel.domain.flora.FloraReminderTypes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PushContentSanitizerTest {

    @Test
    void scheduleChanged_usesGenericCopy() {
        var copy = PushContentSanitizer.forType(FloraReminderTypes.SCHEDULE_CHANGED);
        assertTrue(copy.title().contains("cập nhật"));
        assertFalse(copy.body().toLowerCase().contains("booking"));
        assertFalse(copy.body().contains("@"));
    }

    @Test
    void reminder_usesSafeCopyWithoutLocation() {
        var copy = PushContentSanitizer.forType(FloraReminderTypes.TOUR_REMINDER_15_MINUTES);
        assertFalse(copy.body().contains("tập trung tại"));
        assertFalse(copy.body().matches(".*\\d{2}:\\d{2}.*"));
    }
}
