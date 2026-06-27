package com.flourishtravel.domain.tour.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * VietMap API key — đọc từ nhiều tên biến env (tránh DO set VIETMAP_API_KEY rỗng chặn fallback).
 */
@Component
@Getter
public class VietMapProperties {

    @Value("${app.vietmap.api-key:}")
    private String apiKey;

    @PostConstruct
    void resolveApiKey() {
        if (isPresent(apiKey)) {
            return;
        }
        apiKey = firstPresent(
                System.getenv("MAP_VIET_ACESS_KEY"),
                System.getenv("MAP_VIET_ACCESS_KEY"),
                System.getenv("VIETMAP_API_KEY"));
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
