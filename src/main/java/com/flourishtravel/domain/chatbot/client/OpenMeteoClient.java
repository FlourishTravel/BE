package com.flourishtravel.domain.chatbot.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * Gọi Open-Meteo API (geocoding + weather forecast). Không cần API key.
 */
@Component
@Slf4j
public class OpenMeteoClient {

    private static final String GEOCODING_URL = "https://geocoding-api.open-meteo.com/v1/search";
    private static final String FORECAST_URL = "https://api.open-meteo.com/v1/forecast";

    private final WebClient webClient;

    public OpenMeteoClient(@Qualifier("openMeteoWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    /** Trả về [lat, lon] hoặc null nếu không tìm thấy. */
    @SuppressWarnings("unchecked")
    public double[] geocode(String cityName) {
        if (cityName == null || cityName.isBlank()) return null;
        String name = cityName.trim().replace(" ", "+");
        try {
            Map<String, Object> body = webClient.get()
                    .uri(GEOCODING_URL + "?name=" + name + "&count=1&language=vi")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            if (body == null || !(body.get("results") instanceof java.util.List<?> list) || list.isEmpty())
                return null;
            Object first = list.get(0);
            if (!(first instanceof Map<?, ?> m)) return null;
            Object lat = m.get("latitude");
            Object lon = m.get("longitude");
            if (lat instanceof Number lt && lon instanceof Number ln)
                return new double[]{lt.doubleValue(), ln.doubleValue()};
        } catch (Exception e) {
            log.debug("Open-Meteo geocoding failed for {}: {}", cityName, e.getMessage());
        }
        return null;
    }

    /** Lấy dự báo thời tiết theo lat,lon. Trả map có current, daily hoặc null. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getForecast(double lat, double lon) {
        try {
            String uri = FORECAST_URL + "?latitude=" + lat + "&longitude=" + lon
                    + "&current=temperature_2m,weather_code"
                    + "&daily=weather_code,temperature_2m_max,temperature_2m_min"
                    + "&timezone=Asia/Ho_Chi_Minh&forecast_days=3";
            Map<String, Object> body = webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            return body;
        } catch (Exception e) {
            log.debug("Open-Meteo forecast failed: {}", e.getMessage());
        }
        return null;
    }
}
