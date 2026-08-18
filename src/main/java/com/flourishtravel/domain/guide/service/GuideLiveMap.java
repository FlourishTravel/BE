package com.flourishtravel.domain.guide.service;

import com.flourishtravel.domain.tour.entity.TourSession;
import com.flourishtravel.domain.tour.service.TourSessionStatusResolver;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Cửa sổ bản đồ realtime HDV: chỉ khi session đang diễn ra theo lịch VN.
 */
public final class GuideLiveMap {

    public static final Duration STALE_AFTER = Duration.ofMinutes(20);

    private GuideLiveMap() {
    }

    public static boolean isLive(TourSession session, LocalDate today) {
        return TourSessionStatusResolver.isOngoing(session, today);
    }

    public static String offlineReason(TourSession session, LocalDate today) {
        String status = TourSessionStatusResolver.resolveEffectiveStatus(session, today);
        return switch (status) {
            case TourSessionStatusResolver.CANCELLED ->
                    "Chuyến đã hủy — không theo dõi vị trí khách.";
            case TourSessionStatusResolver.COMPLETED ->
                    "Chuyến đã kết thúc — bản đồ realtime chỉ hiện trong ngày tour.";
            default ->
                    "Chuyến chưa bắt đầu — bản đồ realtime sẽ mở khi đoàn đang đi.";
        };
    }

    public static boolean isStale(Instant capturedAt, Instant now) {
        if (capturedAt == null || now == null) {
            return true;
        }
        return capturedAt.isBefore(now.minus(STALE_AFTER));
    }
}
