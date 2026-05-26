package com.flourishtravel.domain.planner.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class PlannerActivityDto {
    private UUID id;
    private String time;
    private String title;
    private String description;
    private String imageUrl;
    private String category;
    private String locationName;
    private Double latitude;
    private Double longitude;
    private Boolean floraRecommended;
    private String priceLabel;
}
