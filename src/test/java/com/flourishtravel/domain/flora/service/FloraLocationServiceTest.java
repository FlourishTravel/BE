package com.flourishtravel.domain.flora.service;

import com.flourishtravel.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FloraLocationServiceTest {

    @Test
    void validateCoordinates_rejectsOutOfRange() {
        assertThrows(BadRequestException.class, () -> FloraLocationService.validateCoordinates(100.0, 0.0));
    }

    @Test
    void validateCoordinates_acceptsValid() {
        assertDoesNotThrow(() -> FloraLocationService.validateCoordinates(10.77, 106.70));
    }

    @Test
    void haversineMeters_samePointIsZero() {
        double d = FloraLocationService.haversineMeters(10.0, 106.0, 10.0, 106.0);
        assertEquals(0.0, d, 1.0);
    }
}
