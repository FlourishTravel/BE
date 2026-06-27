package com.flourishtravel.domain.tour.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class VietMapClientParsingTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void extractResultArray_parsesTopLevelSearchArray() throws Exception {
        JsonNode root = objectMapper.readTree("""
                [{"ref_id":"geocode:abc","name":"Bến Xe Khách Cần Thơ"}]
                """);

        JsonNode results = invokeExtractResultArray(root);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Bến Xe Khách Cần Thơ", results.get(0).get("name").asText());
    }

    @Test
    void readCoordinate_parsesPlaceLatLng() throws Exception {
        JsonNode place = objectMapper.readTree("""
                {"lat":10.023619753000048,"lng":105.76231478400007,"name":"Bến Xe Khách Cần Thơ"}
                """);

        Double lat = invokeReadCoordinate(place, "lat", "latitude");
        Double lng = invokeReadCoordinate(place, "lng", "longitude", "lon");

        assertEquals(10.023619753000048, lat);
        assertEquals(105.76231478400007, lng);
    }

    private static JsonNode invokeExtractResultArray(JsonNode root) throws Exception {
        Method method = VietMapClient.class.getDeclaredMethod("extractResultArray", JsonNode.class);
        method.setAccessible(true);
        return (JsonNode) method.invoke(null, root);
    }

    private static Double invokeReadCoordinate(JsonNode node, String... keys) throws Exception {
        Method method = VietMapClient.class.getDeclaredMethod("readCoordinate", JsonNode.class, String[].class);
        method.setAccessible(true);
        return (Double) method.invoke(null, node, (Object) keys);
    }
}
