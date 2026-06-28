package com.flourishtravel.domain.tour.service;

import com.flourishtravel.domain.tour.entity.TourSession;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TourSessionStatusResolverTest {

    @Test
    void beforeStartDate_isScheduled() {
        TourSession session = session(LocalDate.of(2026, 6, 27), LocalDate.of(2026, 6, 28), "scheduled");
        assertEquals("scheduled", TourSessionStatusResolver.resolveEffectiveStatus(session, LocalDate.of(2026, 6, 26)));
    }

    @Test
    void onStartAndEndDates_isOngoing() {
        TourSession session = session(LocalDate.of(2026, 6, 27), LocalDate.of(2026, 6, 28), "scheduled");
        assertEquals("ongoing", TourSessionStatusResolver.resolveEffectiveStatus(session, LocalDate.of(2026, 6, 27)));
        assertEquals("ongoing", TourSessionStatusResolver.resolveEffectiveStatus(session, LocalDate.of(2026, 6, 28)));
    }

    @Test
    void afterEndDate_isCompleted() {
        TourSession session = session(LocalDate.of(2026, 6, 27), LocalDate.of(2026, 6, 28), "scheduled");
        assertEquals("completed", TourSessionStatusResolver.resolveEffectiveStatus(session, LocalDate.of(2026, 6, 29)));
    }

    @Test
    void cancelledStaysCancelled() {
        TourSession session = session(LocalDate.of(2026, 6, 27), LocalDate.of(2026, 6, 28), "cancelled");
        assertEquals("cancelled", TourSessionStatusResolver.resolveEffectiveStatus(session, LocalDate.of(2026, 6, 27)));
    }

    @Test
    void persistOngoingDuringTrip() {
        TourSession session = session(LocalDate.of(2026, 6, 27), LocalDate.of(2026, 6, 28), "scheduled");
        assertEquals("ongoing", TourSessionStatusResolver.resolveStatusToPersist(session, LocalDate.of(2026, 6, 27)));
    }

    private static TourSession session(LocalDate start, LocalDate end, String status) {
        return TourSession.builder().startDate(start).endDate(end).status(status).build();
    }
}
