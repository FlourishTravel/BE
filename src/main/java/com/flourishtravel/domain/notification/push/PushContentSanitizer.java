package com.flourishtravel.domain.notification.push;

import com.flourishtravel.domain.flora.FloraReminderTypes;

import java.util.Map;

public final class PushContentSanitizer {

    private PushContentSanitizer() {}

    private static final Map<String, String[]> SAFE_COPY = Map.of(
            FloraReminderTypes.TOUR_REMINDER_30_MINUTES, new String[]{
                    "Flora nhắc bạn kiểm tra hành trình",
                    "Bạn có một nhắc nhở quan trọng trong chuyến đi."
            },
            FloraReminderTypes.TOUR_REMINDER_15_MINUTES, new String[]{
                    "Flora nhắc bạn kiểm tra hành trình",
                    "Bạn có một nhắc nhở quan trọng trong chuyến đi."
            },
            FloraReminderTypes.TOUR_REMINDER_5_MINUTES, new String[]{
                    "Flora nhắc bạn kiểm tra hành trình",
                    "Bạn có một nhắc nhở quan trọng trong chuyến đi."
            },
            FloraReminderTypes.RETURN_TO_BUS_ALERT, new String[]{
                    "Flora nhắc bạn kiểm tra hành trình",
                    "Bạn có một nhắc nhở quan trọng trong chuyến đi."
            },
            FloraReminderTypes.SCHEDULE_CHANGED, new String[]{
                    "Lịch trình tour đã được cập nhật",
                    "Mở ứng dụng để xem giờ và điểm tập trung mới nhất."
            },
            FloraReminderTypes.POST_TOUR_FEEDBACK, new String[]{
                    "Flora có một cập nhật mới",
                    "Mở Flourish-Travel để xem thông tin hành trình mới nhất."
            }
    );

    public record SafePushCopy(String title, String body) {}

    public static SafePushCopy forType(String type) {
        String[] copy = SAFE_COPY.get(type);
        if (copy != null) {
            return new SafePushCopy(copy[0], copy[1]);
        }
        return new SafePushCopy(
                "Flora có một cập nhật mới",
                "Mở Flourish-Travel để xem thông tin hành trình mới nhất.");
    }
}
