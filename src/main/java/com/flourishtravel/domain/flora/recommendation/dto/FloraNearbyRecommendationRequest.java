package com.flourishtravel.domain.flora.recommendation.dto;

import lombok.Data;

import java.util.List;

@Data
public class FloraNearbyRecommendationRequest {

    private Double latitude;
    private Double longitude;
    /** true khi khách chủ động chia sẻ GPS lúc bấm "Gợi ý gần đây" (web/mobile). */
    private Boolean locationConsent;
    private Integer radiusMeters;
    private Integer limit;
    private List<String> categories;
}
