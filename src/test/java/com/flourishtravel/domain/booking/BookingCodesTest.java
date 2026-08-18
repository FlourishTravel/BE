package com.flourishtravel.domain.booking;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingCodesTest {

    @Test
    void fromId_usesUppercaseFtPrefix() {
        UUID id = UUID.fromString("aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee");
        assertEquals("FT-AAAAAAAA", BookingCodes.fromId(id));
    }

    @Test
    void fromId_nullIsNull() {
        assertNull(BookingCodes.fromId(null));
    }

    @Test
    void parseUuid_acceptsHyphenatedId() {
        UUID id = UUID.fromString("aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee");
        assertEquals(id, BookingCodes.parseUuid(id.toString()).orElseThrow());
    }

    @Test
    void parseUuid_rejectsBookingCode() {
        assertTrue(BookingCodes.parseUuid("FT-AAAAAAAA").isEmpty());
    }

    @Test
    void normalize_acceptsLowercasePaymentStyleCode() {
        assertEquals("FT-A1B2C3D4", BookingCodes.normalize("ft-a1b2c3d4"));
    }

    @Test
    void normalize_acceptsUuid() {
        UUID id = UUID.fromString("aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee");
        assertEquals("FT-AAAAAAAA", BookingCodes.normalize(id.toString()));
    }

    @Test
    void normalize_rejectsGarbage() {
        assertNull(BookingCodes.normalize("not-a-booking"));
    }
}
