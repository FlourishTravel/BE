package com.flourishtravel.domain.tour.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Goong REST API key — admin geocode itinerary.
 * @see <a href="https://account.goong.io">account.goong.io</a>
 */
@Component
@Getter
public class GoongProperties {

    @Value("${app.goong.api-key:}")
    private String apiKey;

    @PostConstruct
    void resolveApiKey() {
        if (isPresent(apiKey)) {
            return;
        }
        apiKey = firstPresent(
                System.getenv("GOONG_API_KEY"),
                System.getenv("GOONG_MAPS_API_KEY"));
        if (apiKey == null) {
            apiKey = "";
        }
    }

    public boolean isConfigured() {
        return isPresent(apiKey);
    }

    public String getApiKey() {
        return apiKey == null ? "" : apiKey.trim();
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    private static String firstPresent(String... candidates) {
        for (String c : candidates) {
            if (isPresent(c)) {
                return c.trim();
            }
        }
        return null;
    }
}
