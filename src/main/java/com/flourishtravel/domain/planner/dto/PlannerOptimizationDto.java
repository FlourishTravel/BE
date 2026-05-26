package com.flourishtravel.domain.planner.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PlannerOptimizationDto {
    private String status;
    private int progressPercent;
    private List<String> steps;
    private List<Boolean> stepsCompleted;
}
