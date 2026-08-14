package com.flourishtravel.domain.tour.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoongClientParsingTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void hitFromGoogleLike_parsesGeocodeResult() throws Exception {
        JsonNode result = objectMapper.readTree("""
                {
                  "formatted_address": "91 Trung Kính, Trung Hòa, Cầu Giấy, Hà Nội",
                  "geometry": { "location": { "lat": 21.0137625240001, "lng": 105.798267363 } }
                }
                """);

        Optional<GoongClient.GoongGeocodeHit> hit = GoongClient.hitFromGoogleLike(result, "fallback");

        assertTrue(hit.isPresent());
        assertEquals(21.0137625240001, hit.get().latitude());
        assertEquals(105.798267363, hit.get().longitude());
        assertEquals("91 Trung Kính, Trung Hòa, Cầu Giấy, Hà Nội", hit.get().label());
    }

    @Test
    void isDeniedStatus_detectsRequestDenied() throws Exception {
        JsonNode root = objectMapper.readTree("""
                {"status":"REQUEST_DENIED","error_message":"The provided API key is invalid."}
                """);
        assertTrue(GoongClient.isDeniedStatus(root));
        assertFalse(GoongClient.isDeniedStatus(objectMapper.readTree("""
                {"status":"OK","results":[]}
                """)));
    }
}
