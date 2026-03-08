package com.flourishtravel.domain.chatbot.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Gọi Overpass API (OpenStreetMap) để tìm địa điểm quanh tọa độ. Không cần API key.
 */
@Component
@Slf4j
public class OverpassClient {

    private static final String INTERPRETER_URL = "https://overpass-api.de/api/interpreter";

    private final WebClient webClient;

    public OverpassClient(@Qualifier("overpassWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Tìm POI (restaurant, cafe...) trong bán kính 2km. Trả phần tử đầu tiên có name.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> findNearbyPoi(double lat, double lon, String amenityType) {
        String amenity = "restaurant|cafe|fast_food";
        if (amenityType != null && !amenityType.isBlank()) {
            String lower = amenityType.toLowerCase();
            if (lower.contains("cafe") || lower.contains("cà phê")) amenity = "cafe";
            else if (lower.contains("ăn") || lower.contains("quán")) amenity = "restaurant|fast_food";
        }
        String query = "[out:json][timeout:10];"
                + "node(around:2000," + lat + "," + lon + ")[\"amenity\"~\"" + amenity + "\"];"
                + "out body 5;";
        try {
            String formBody = "data=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
            Map<String, Object> body = webClient.post()
                    .uri(INTERPRETER_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue(formBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            if (body == null || !(body.get("elements") instanceof List<?> elements) || elements.isEmpty())
                return null;
            for (Object el : elements) {
                if (!(el instanceof Map<?, ?> m)) continue;
                Object tags = m.get("tags");
                if (tags instanceof Map<?, ?> t && t.get("name") != null) {
                    return (Map<String, Object>) m;
                }
            }
            return elements.get(0) instanceof Map ? (Map<String, Object>) elements.get(0) : null;
        } catch (Exception e) {
            log.debug("Overpass query failed: {}", e.getMessage());
        }
        return null;
    }
}
