package com.flourishtravel.domain.planner.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlannerSuggestionDto {
    private String type;
    private String message;
    private String currentActivityTitle;
    private String suggestedActivityTitle;
    private String suggestedActivityDescription;
    private String suggestedImageUrl;
}
