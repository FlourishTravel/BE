package com.flourishtravel.domain.flora.recommendation;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.flora.recommendations")
public class FloraRecommendationProperties {

    private int defaultRadiusMeters = 1000;
    private int maxRadiusMeters = 3000;
    private int defaultLimit = 5;
    private int maxLimit = 10;
    private int estimatedWalkingSpeedMetersPerMinute = 60;
    private RateLimit rateLimit = new RateLimit();
    private Map<String, Integer> defaultVisitMinutes = defaultVisitMap();

    private static Map<String, Integer> defaultVisitMap() {
        Map<String, Integer> m = new LinkedHashMap<>();
        m.put("restaurant", 35);
        m.put("cafe", 30);
        m.put("attraction", 45);
        m.put("photo-spot", 20);
        m.put("shopping", 30);
        m.put("restroom", 10);
        return m;
    }

    @Getter
    @Setter
    public static class RateLimit {
        private int requestsPerMinute = 10;
    }
}
