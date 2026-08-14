package com.flourishtravel.domain.tour.service;

import com.flourishtravel.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    void rejectsEndBeforeStart() {
        assertThrows(BadRequestException.class, () ->
                TourService.resolveSessionEndDate(
                        LocalDate.of(2026, 9, 5),
                        LocalDate.of(2026, 9, 1),
                        5));
    }
}
