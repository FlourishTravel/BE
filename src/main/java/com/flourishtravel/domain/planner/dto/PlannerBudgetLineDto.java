package com.flourishtravel.domain.planner.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlannerBudgetLineDto {
    private String category;
    private String label;
    private Long amountVnd;
}
