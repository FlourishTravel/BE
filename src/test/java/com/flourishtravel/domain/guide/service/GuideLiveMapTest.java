package com.flourishtravel.domain.guide.service;

import com.flourishtravel.domain.tour.entity.TourSession;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuideLiveMapTest {

    @Test
    void isLive_onlyDuringTripDates() {
        TourSession session = TourSession.builder()
                .startDate(LocalDate.of(2026, 8, 18))
                .endDate(LocalDate.of(2026, 8, 20))
                .status("scheduled")
                .build();
        assertFalse(GuideLiveMap.isLive(session, LocalDate.of(2026, 8, 17)));
        assertTrue(GuideLiveMap.isLive(session, LocalDate.of(2026, 8, 18)));
        assertFalse(GuideLiveMap.isLive(session, LocalDate.of(2026, 8, 21)));
    }

    @Test
    void offlineReason_beforeStart() {
        TourSession session = TourSession.builder()
                .startDate(LocalDate.of(2026, 8, 18))
                .endDate(LocalDate.of(2026, 8, 20))
                .status("scheduled")
                .build();
        assertEquals(
                "Chuyến chưa bắt đầu — bản đồ realtime sẽ mở khi đoàn đang đi.",
                GuideLiveMap.offlineReason(session, LocalDate.of(2026, 8, 17)));
    }

    @Test
    void offlineReason_afterEnd() {
        TourSession session = TourSession.builder()
                .startDate(LocalDate.of(2026, 8, 18))
                .endDate(LocalDate.of(2026, 8, 20))
                .status("scheduled")
                .build();
        assertEquals(
                "Chuyến đã kết thúc — bản đồ realtime chỉ hiện trong ngày tour.",
                GuideLiveMap.offlineReason(session, LocalDate.of(2026, 8, 21)));
    }

    @Test
    void isStale_afterTwentyMinutes() {
        Instant now = Instant.parse("2026-08-18T04:00:00Z");
        assertFalse(GuideLiveMap.isStale(now.minusSeconds(10 * 60), now));
        assertTrue(GuideLiveMap.isStale(now.minusSeconds(21 * 60), now));
    }
}
