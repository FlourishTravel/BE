package com.flourishtravel.domain.planner.dto;

import lombok.Data;

import java.util.List;

@Data
public class PlannerBudgetRequest {
    private PlannerGenerateRequest request;
    private List<PlannerDayDto> days;
}
