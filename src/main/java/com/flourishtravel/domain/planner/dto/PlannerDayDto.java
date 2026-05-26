package com.flourishtravel.domain.planner.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PlannerDayDto {
    private int dayNumber;
    private String label;
    private String destinationName;
    private List<PlannerActivityDto> activities;
}
