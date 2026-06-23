package com.flourishtravel.domain.flora.recommendation.dto;

import lombok.Data;

import java.util.List;

@Data
public class FloraNearbyRecommendationRequest {

    private Double latitude;
    private Double longitude;
    private Integer radiusMeters;
    private Integer limit;
    private List<String> categories;
}
