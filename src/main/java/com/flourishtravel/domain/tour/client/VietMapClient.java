package com.flourishtravel.domain.tour.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flourishtravel.domain.tour.config.VietMapProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Iterator;
import java.util.Optional;

/**
 * VietMap Maps API (Search v4 + Place v4) — geocoding địa chỉ Việt Nam.
 * @see <a href="https://maps.vietmap.vn/docs/map-api/geocode-version/geocode-v4/">Search v4</a>
 */
@Component
@Slf4j
public class VietMapClient {

    private static final String SEARCH_V4 = "https://maps.vietmap.vn/api/search/v4";
    private static final String PLACE_V4 = "https://maps.vietmap.vn/api/place/v4";

    private final VietMapProperties properties;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    public VietMapClient(
            VietMapProperties properties,
            ObjectMapper objectMapper,
            @Qualifier("vietMapWebClient") WebClient webClient) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.webClient = webClient;
    }

    public boolean isConfigured() {
        return properties.getApiKey() != null && !properties.getApiKey().isBlank();
    }

    public Optional<VietMapGeocodeHit> geocode(String text) {
        if (!isConfigured() || text == null || text.isBlank()) {
            return Optional.empty();
        }
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(SEARCH_V4)
                    .queryParam("apikey", properties.getApiKey())
                    .queryParam("text", text.trim())
                    .queryParam("display_type", 6)
                    .build()
                    .encode()
                    .toUri();

            String raw = fetchBody(uri);
            if (raw == null || raw.isBlank()) {
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(raw);
            if (root.isObject() && hasErrorCode(root)) {
                log.warn("VietMap search error for '{}': {}", text, raw);
                return Optional.empty();
            }

            JsonNode results = extractResultArray(root);
            if (results == null || !results.isArray() || results.isEmpty()) {
                return Optional.empty();
            }

            JsonNode first = results.get(0);
            Double lat = readCoordinate(first, "lat", "latitude");
            Double lng = readCoordinate(first, "lng", "longitude", "lon");
            String refId = readText(first, "ref_id", "refid");
            String label = readText(first, "name", "display", "address");
            if (label == null || label.isBlank()) {
                label = text.trim();
            }

            if (lat != null && lng != null) {
                return Optional.of(new VietMapGeocodeHit(lat, lng, label));
            }
            if (refId != null && !refId.isBlank()) {
                return resolvePlace(refId, label);
            }
            return Optional.empty();
        } catch (Exception e) {
            log.warn("VietMap search failed for '{}': {}", text, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<VietMapGeocodeHit> resolvePlace(String refId, String fallbackLabel) {
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(PLACE_V4)
                    .queryParam("apikey", properties.getApiKey())
                    .queryParam("refid", refId)
                    .build()
                    .encode()
                    .toUri();

            String raw = fetchBody(uri);
            if (raw == null || raw.isBlank()) {
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(raw);
            if (root.isObject() && hasErrorCode(root)) {
                log.warn("VietMap place error for refId={}: {}", refId, raw);
                return Optional.empty();
            }
            JsonNode place = root.isObject() && root.has("data") ? root.get("data") : root;
            Double lat = readCoordinate(place, "lat", "latitude");
            Double lng = readCoordinate(place, "lng", "longitude", "lon");
            String label = readText(place, "name", "display", "address");
            if (label == null || label.isBlank()) {
                label = fallbackLabel;
            }
            if (lat == null || lng == null) {
                return Optional.empty();
            }
            return Optional.of(new VietMapGeocodeHit(lat, lng, label));
        } catch (Exception e) {
            log.warn("VietMap place failed for refId={}: {}", refId, e.getMessage());
            return Optional.empty();
        }
    }

    private static JsonNode extractResultArray(JsonNode root) {
        if (root == null || root.isNull()) {
            return null;
        }
        if (root.isArray()) {
            return root;
        }
        for (String key : new String[]{"data", "list", "results"}) {
            JsonNode node = root.get(key);
            if (node == null) {
                continue;
            }
            if (node.isArray()) {
                return node;
            }
            if (node.isObject()) {
                JsonNode fromObject = firstArrayInObject(node);
                if (fromObject != null) {
                    return fromObject;
                }
            }
        }
        return null;
    }

    private static JsonNode firstArrayInObject(JsonNode object) {
        Iterator<JsonNode> it = object.elements();
        while (it.hasNext()) {
            JsonNode child = it.next();
            if (child.isArray() && !child.isEmpty()) {
                return child;
            }
        }
        return null;
    }

    private static Double readCoordinate(JsonNode node, String... keys) {
        if (node == null || node.isNull()) {
            return null;
        }
        for (String key : keys) {
            JsonNode value = node.get(key);
            if (value != null && value.isNumber()) {
                return value.doubleValue();
            }
            if (value != null && value.isTextual()) {
                try {
                    return Double.parseDouble(value.asText());
                } catch (NumberFormatException ignored) {
                    // try next key
                }
            }
        }
        return null;
    }

    private String fetchBody(URI uri) {
        try {
            return webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.warn("VietMap HTTP {} for {}: {}", e.getStatusCode().value(), uri.getPath(), e.getResponseBodyAsString());
            return null;
        }
    }

    private static boolean hasErrorCode(JsonNode root) {
        JsonNode code = root.get("code");
        if (code == null || code.isNull()) {
            return false;
        }
        String value = code.asText("");
        return !value.isBlank() && !"OK".equalsIgnoreCase(value) && !"00".equals(value);
    }

    private static String readText(JsonNode node, String... keys) {
        if (node == null || node.isNull()) {
            return null;
        }
        for (String key : keys) {
            JsonNode value = node.get(key);
            if (value != null && value.isTextual() && !value.asText().isBlank()) {
                return value.asText();
            }
        }
        return null;
    }

    public record VietMapGeocodeHit(double latitude, double longitude, String label) {}
}
