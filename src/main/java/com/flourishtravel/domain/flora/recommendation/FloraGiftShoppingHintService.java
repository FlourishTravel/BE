package com.flourishtravel.domain.flora.recommendation;

import com.flourishtravel.domain.chatbot.client.OverpassClient;
import com.flourishtravel.domain.chatbot.dto.ChatbotRequest;
import com.flourishtravel.domain.chatbot.security.ChatbotSecurityService;
import com.flourishtravel.domain.flora.service.FloraContextBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Builds an LLM hint for in-store gift asks. Optionally names the nearest OSM shop
 * when the user granted location but did not say the venue.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FloraGiftShoppingHintService {

    private static final int NEAREST_SHOP_RADIUS_M = 150;
    private static final long OVERPASS_TIMEOUT_MS = 2000;
    private static final Set<String> PREFERRED_SHOP_TYPES = Set.of(
            "supermarket", "mall", "department_store", "convenience",
            "gift", "clothes", "chemist", "cosmetics", "bakery");

    private final OverpassClient overpassClient;
    private final ChatbotSecurityService chatbotSecurityService;

    public String buildHint(ChatbotRequest request, UUID userId) {
        String content = FloraContextBuilder.resolveContent(request);
        FloraGiftShoppingAdvice.GiftAsk ask = FloraGiftShoppingAdvice.parse(content);
        if (!ask.giftIntent()) return "";

        String gpsVenue = null;
        if (ask.venue() == null && chatbotSecurityService.shouldIncludeLocationInHint(request, userId)) {
            gpsVenue = resolveNearestShopName(request.getLatitude(), request.getLongitude());
        }
        return FloraGiftShoppingAdvice.buildLlmHint(ask, gpsVenue);
    }

    String resolveNearestShopName(Double lat, Double lon) {
        if (lat == null || lon == null) return null;
        try {
            return CompletableFuture.supplyAsync(() -> lookupNearestShop(lat, lon))
                    .orTimeout(OVERPASS_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    .exceptionally(ex -> {
                        log.debug("Nearest shop lookup skipped: {}", ex.getMessage());
                        return null;
                    })
                    .join();
        } catch (Exception ex) {
            log.debug("Nearest shop lookup failed: {}", ex.getMessage());
            return null;
        }
    }

    private String lookupNearestShop(double lat, double lon) {
        List<Map<String, Object>> pois = overpassClient.findNearbyPois(
                lat, lon, NEAREST_SHOP_RADIUS_M, List.of("SHOPPING"), 8);
        String bestName = null;
        double bestScore = Double.MAX_VALUE;
        for (Map<String, Object> poi : pois) {
            String name = poiName(poi);
            double[] coords = poiCoords(poi);
            if (name == null || coords == null) continue;
            double meters = haversineMeters(lat, lon, coords[0], coords[1]);
            if (meters > NEAREST_SHOP_RADIUS_M) continue;
            double score = meters + (preferredShop(poi) ? 0 : 40);
            if (score < bestScore) {
                bestScore = score;
                bestName = name;
            }
        }
        return bestName;
    }

    @SuppressWarnings("unchecked")
    private static String poiName(Map<String, Object> poi) {
        Object tagsObj = poi.get("tags");
        if (!(tagsObj instanceof Map<?, ?> tags)) return null;
        for (String key : List.of("name", "name:en", "name:th")) {
            Object v = tags.get(key);
            if (v != null && !v.toString().isBlank()) return v.toString().trim();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static boolean preferredShop(Map<String, Object> poi) {
        Object tagsObj = poi.get("tags");
        if (!(tagsObj instanceof Map<?, ?> tags)) return false;
        Object shop = tags.get("shop");
        if (shop == null) return false;
        return PREFERRED_SHOP_TYPES.contains(shop.toString().toLowerCase(Locale.ROOT));
    }

    @SuppressWarnings("unchecked")
    private static double[] poiCoords(Map<String, Object> poi) {
        Double lat = asDouble(poi.get("lat"));
        Double lon = asDouble(poi.get("lon"));
        if (lat == null && poi.get("center") instanceof Map<?, ?> center) {
            lat = asDouble(center.get("lat"));
            lon = asDouble(center.get("lon"));
        }
        if (lat == null || lon == null) return null;
        return new double[]{lat, lon};
    }

    private static Double asDouble(Object v) {
        if (v instanceof Number n) return n.doubleValue();
        if (v instanceof String s) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371000;
        double p1 = Math.toRadians(lat1);
        double p2 = Math.toRadians(lat2);
        double dp = Math.toRadians(lat2 - lat1);
        double dl = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dp / 2) * Math.sin(dp / 2)
                + Math.cos(p1) * Math.cos(p2) * Math.sin(dl / 2) * Math.sin(dl / 2);
        return 2 * r * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
