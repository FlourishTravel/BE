package com.flourishtravel.domain.tour.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flourishtravel.domain.tour.config.GoongProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.util.Optional;

/**
 * Goong Maps REST API — geocoding địa chỉ / POI Việt Nam (thay VietMap).
 * @see <a href="https://docs.goong.io/rest/geocode/">Geocode</a>
 * @see <a href="https://docs.goong.io/rest/place/">Places</a>
 */
@Component
@Slf4j
public class GoongClient {

    private static final String HOST = "rsapi.goong.io";

    private final GoongProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private boolean lastAuthError;

    public GoongClient(
            GoongProperties properties,
            ObjectMapper objectMapper,
            @Qualifier("goongRestClient") RestClient restClient) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = restClient;
    }

    public boolean isConfigured() {
        return properties.isConfigured();
    }

    public boolean hadAuthError() {
        return lastAuthError;
    }

    public Optional<GoongGeocodeHit> geocode(String text) {
        if (!isConfigured() || text == null || text.isBlank()) {
            return Optional.empty();
        }
        lastAuthError = false;
        String query = text.trim();

        Optional<GoongGeocodeHit> fromPlace = geocodeViaAutocomplete(query);
        if (fromPlace.isPresent()) {
            return fromPlace;
        }
        return geocodeViaAddress(query);
    }

    private Optional<GoongGeocodeHit> geocodeViaAutocomplete(String query) {
        try {
            String raw = fetch("autocomplete", uriBuilder -> uriBuilder
                    .scheme("https")
                    .host(HOST)
                    .path("/Place/AutoComplete")
                    .queryParam("api_key", properties.getApiKey())
                    .queryParam("input", query)
                    .queryParam("limit", 3)
                    .build());
            JsonNode root = parseRoot(raw);
            if (root == null || isDenied(root)) {
                return Optional.empty();
            }
            JsonNode predictions = root.get("predictions");
            if (predictions == null || !predictions.isArray() || predictions.isEmpty()) {
                return Optional.empty();
            }
            int limit = Math.min(predictions.size(), 3);
            for (int i = 0; i < limit; i++) {
                JsonNode prediction = predictions.get(i);
                String placeId = text(prediction, "place_id");
                String fallbackLabel = firstNonBlank(text(prediction, "description"), query);
                if (placeId == null) {
                    continue;
                }
                Optional<GoongGeocodeHit> detail = fetchPlaceDetail(placeId, fallbackLabel);
                if (detail.isPresent()) {
                    return detail;
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Goong autocomplete failed for '{}': {}", query, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<GoongGeocodeHit> fetchPlaceDetail(String placeId, String fallbackLabel) {
        try {
            String raw = fetch("place-detail", uriBuilder -> uriBuilder
                    .scheme("https")
                    .host(HOST)
                    .path("/Place/Detail")
                    .queryParam("api_key", properties.getApiKey())
                    .queryParam("place_id", placeId)
                    .build());
            JsonNode root = parseRoot(raw);
            if (root == null || isDenied(root)) {
                return Optional.empty();
            }
            JsonNode result = root.get("result");
            if (result == null || result.isNull()) {
                JsonNode results = root.get("results");
                if (results != null && results.isArray() && !results.isEmpty()) {
                    result = results.get(0);
                }
            }
            return hitFromGoogleLike(result, fallbackLabel);
        } catch (Exception e) {
            log.warn("Goong place detail failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<GoongGeocodeHit> geocodeViaAddress(String query) {
        try {
            String raw = fetch("geocode", uriBuilder -> uriBuilder
                    .scheme("https")
                    .host(HOST)
                    .path("/geocode")
                    .queryParam("api_key", properties.getApiKey())
                    .queryParam("address", query)
                    .build());
            JsonNode root = parseRoot(raw);
            if (root == null || isDenied(root)) {
                return Optional.empty();
            }
            JsonNode results = root.get("results");
            if (results == null || !results.isArray() || results.isEmpty()) {
                return Optional.empty();
            }
            int limit = Math.min(results.size(), 3);
            for (int i = 0; i < limit; i++) {
                Optional<GoongGeocodeHit> hit = hitFromGoogleLike(results.get(i), query);
                if (hit.isPresent()) {
                    return hit;
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Goong geocode failed for '{}': {}", query, e.getMessage());
            return Optional.empty();
        }
    }

    static Optional<GoongGeocodeHit> hitFromGoogleLike(JsonNode node, String fallbackLabel) {
        if (node == null || node.isNull()) {
            return Optional.empty();
        }
        JsonNode location = node.path("geometry").path("location");
        Double lat = readNumber(location, "lat");
        Double lng = readNumber(location, "lng");
        if (lat == null || lng == null) {
            return Optional.empty();
        }
        String label = firstNonBlank(
                text(node, "name"),
                text(node, "formatted_address"),
                fallbackLabel);
        return Optional.of(new GoongGeocodeHit(lat, lng, label));
    }

    static boolean isDeniedStatus(JsonNode root) {
        if (root == null || root.isNull()) {
            return false;
        }
        String status = text(root, "status");
        if (status == null) {
            return false;
        }
        return "REQUEST_DENIED".equalsIgnoreCase(status)
                || "OVER_DAILY_LIMIT".equalsIgnoreCase(status);
    }

    private boolean isDenied(JsonNode root) {
        if (isDeniedStatus(root)) {
            lastAuthError = true;
            log.warn("Goong denied request: {}", root.path("error_message").asText(root.path("status").asText("")));
            return true;
        }
        return false;
    }

    private JsonNode parseRoot(String raw) throws Exception {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return objectMapper.readTree(raw);
    }

    private String fetch(String operation, java.util.function.Function<UriBuilder, URI> uriFn) {
        try {
            return restClient.get()
                    .uri(uriFn)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            if (status == 401 || status == 403) {
                lastAuthError = true;
            }
            log.warn("Goong {} HTTP {}: {}", operation, status, e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.warn("Goong {} request failed: {}", operation, e.getMessage());
            return null;
        }
    }

    static Double readNumber(JsonNode node, String key) {
        if (node == null || node.isNull() || !node.has(key)) {
            return null;
        }
        JsonNode value = node.get(key);
        if (value.isNumber()) {
            return value.doubleValue();
        }
        if (value.isTextual()) {
            try {
                return Double.parseDouble(value.asText());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static String text(JsonNode node, String key) {
        if (node == null || node.isNull()) {
            return null;
        }
        JsonNode value = node.get(key);
        if (value != null && value.isTextual() && !value.asText().isBlank()) {
            return value.asText().trim();
        }
        return null;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    public record GoongGeocodeHit(double latitude, double longitude, String label) {}
}
