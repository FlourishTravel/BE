package com.flourishtravel.domain.tour.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flourishtravel.domain.tour.config.VietMapProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * VietMap Maps API (Search v4 + Place v4) — geocoding địa chỉ Việt Nam.
 * @see <a href="https://maps.vietmap.vn/docs/map-api/geocode-version/geocode-v4/">Search v4</a>
 */
@Component
@Slf4j
public class VietMapClient {

    private static final String HOST = "maps.vietmap.vn";
    private static final String SEARCH_PATH = "/api/search/v4";
    private static final String PLACE_PATH = "/api/place/v4";

    private final VietMapProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private boolean lastAuthError;

    public VietMapClient(
            VietMapProperties properties,
            ObjectMapper objectMapper,
            @Qualifier("vietMapRestClient") RestClient restClient) {
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

    public Optional<VietMapGeocodeHit> geocode(String text) {
        if (!isConfigured() || text == null || text.isBlank()) {
            return Optional.empty();
        }
        lastAuthError = false;
        try {
            String raw = fetchSearch(text.trim());
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

            int limit = Math.min(results.size(), 3);
            for (int i = 0; i < limit; i++) {
                Optional<VietMapGeocodeHit> hit = hitFromSearchResult(results.get(i), text.trim());
                if (hit.isPresent()) {
                    return hit;
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            log.warn("VietMap search failed for '{}': {}", text, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<VietMapGeocodeHit> hitFromSearchResult(JsonNode node, String fallbackText) {
        Optional<VietMapGeocodeHit> fromEntryPoints = coordsFromEntryPoints(node, fallbackText);
        if (fromEntryPoints.isPresent()) {
            return fromEntryPoints;
        }

        Double lat = readCoordinate(node, "lat", "latitude");
        Double lng = readCoordinate(node, "lng", "longitude", "lon");
        String label = readText(node, "name", "display", "address");
        if (label == null || label.isBlank()) {
            label = fallbackText;
        }

        if (lat != null && lng != null) {
            return Optional.of(new VietMapGeocodeHit(lat, lng, label));
        }

        for (String refId : collectRefIds(node)) {
            Optional<VietMapGeocodeHit> fromPlace = resolvePlace(refId, label);
            if (fromPlace.isPresent()) {
                return fromPlace;
            }
        }
        return Optional.empty();
    }

    private Optional<VietMapGeocodeHit> coordsFromEntryPoints(JsonNode node, String fallbackText) {
        JsonNode entryPoints = node.get("entry_points");
        if (entryPoints == null || !entryPoints.isArray() || entryPoints.isEmpty()) {
            return Optional.empty();
        }
        JsonNode first = entryPoints.get(0);
        Double lat = readCoordinate(first, "lat", "latitude");
        Double lng = readCoordinate(first, "lng", "longitude", "lon");
        if (lat == null || lng == null) {
            return Optional.empty();
        }
        String label = readText(node, "name", "display", "address");
        if (label == null || label.isBlank()) {
            label = fallbackText;
        }
        return Optional.of(new VietMapGeocodeHit(lat, lng, label));
    }

    private static List<String> collectRefIds(JsonNode node) {
        Set<String> refIds = new LinkedHashSet<>();
        addRefId(refIds, readText(node, "ref_id", "refid"));
        JsonNode dataNew = node.get("data_new");
        if (dataNew != null && dataNew.isObject()) {
            addRefId(refIds, readText(dataNew, "ref_id", "refid"));
        }
        JsonNode dataOld = node.get("data_old");
        if (dataOld != null && dataOld.isObject()) {
            addRefId(refIds, readText(dataOld, "ref_id", "refid"));
        }
        return new ArrayList<>(refIds);
    }

    private static void addRefId(Set<String> refIds, String refId) {
        if (refId != null && !refId.isBlank()) {
            refIds.add(refId.trim());
        }
    }

    private Optional<VietMapGeocodeHit> resolvePlace(String refId, String fallbackLabel) {
        try {
            String raw = fetchPlace(refId);
            if (raw == null || raw.isBlank()) {
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(raw);
            if (root.isObject() && hasErrorCode(root)) {
                log.warn("VietMap place error refIdPrefix={}: {}", refIdPrefix(refId), raw);
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
            log.warn("VietMap place failed refIdPrefix={}: {}", refIdPrefix(refId), e.getMessage());
            return Optional.empty();
        }
    }

    private String fetchSearch(String text) {
        return fetch("search", uriBuilder -> uriBuilder
                .scheme("https")
                .host(HOST)
                .path(SEARCH_PATH)
                .queryParam("apikey", properties.getApiKey())
                .queryParam("text", text)
                .queryParam("display_type", 6)
                .build());
    }

    private String fetchPlace(String refId) {
        return fetch("place", uriBuilder -> uriBuilder
                .scheme("https")
                .host(HOST)
                .path(PLACE_PATH)
                .queryParam("apikey", properties.getApiKey())
                .queryParam("refid", refId)
                .build());
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
            log.warn("VietMap {} HTTP {}: {}", operation, status, e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.warn("VietMap {} request failed: {}", operation, e.getMessage());
            return null;
        }
    }

    private static String refIdPrefix(String refId) {
        if (refId == null || refId.isBlank()) {
            return "?";
        }
        int colon = refId.indexOf(':');
        if (colon > 0 && colon < 12) {
            return refId.substring(0, colon + 1) + "…";
        }
        return refId.length() > 16 ? refId.substring(0, 16) + "…" : refId;
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
