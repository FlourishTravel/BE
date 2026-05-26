package com.flourishtravel.domain.planner.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class PlannerGenerateResponse {
    private UUID sessionId;
    private String tripSummary;
    private int daysCount;
    private int nightsCount;
    private List<PlannerDayDto> days;
    private List<PlannerActivityDto> activityPool;
    private PlannerBudgetDto budget;
    private PlannerOptimizationDto optimization;
    private PlannerSuggestionDto suggestion;
}
