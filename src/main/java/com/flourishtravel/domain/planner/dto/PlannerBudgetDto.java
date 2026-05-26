package com.flourishtravel.domain.planner.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PlannerBudgetDto {
    private List<PlannerBudgetLineDto> lines;
    private Long totalVnd;
    private Long budgetVnd;
    private Boolean withinBudget;
}
