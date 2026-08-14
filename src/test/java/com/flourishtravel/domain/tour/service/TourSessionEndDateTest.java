package com.flourishtravel.domain.tour.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TourSessionEndDateTest {

    @Test
    void usesProvidedEndDateWhenValid() {
        LocalDate start = LocalDate.of(2026, 9, 1);
        LocalDate end = LocalDate.of(2026, 9, 5);
        assertEquals(end, TourService.resolveSessionEndDate(start, end, 10));
    }

    @Test
    void computesEndDateFromDurationDays() {
        LocalDate start = LocalDate.of(2026, 9, 1);
        assertEquals(LocalDate.of(2026, 9, 5), TourService.resolveSessionEndDate(start, null, 5));
    }

    @Test
    void sameDayWhenDurationMissingOrOne() {
        LocalDate start = LocalDate.of(2026, 9, 1);
        assertEquals(start, TourService.resolveSessionEndDate(start, null, null));
        assertEquals(start, TourService.resolveSessionEndDate(start, null, 1));
    }

    @Test
    void recomputesWhenEndBeforeStart() {
        LocalDate start = LocalDate.of(2026, 9, 1);
        assertEquals(LocalDate.of(2026, 9, 5), TourService.resolveSessionEndDate(
                start, LocalDate.of(2026, 8, 31), 5));
        assertEquals(start, TourService.resolveSessionEndDate(
                start, LocalDate.of(2026, 8, 31), 1));
    }
}
