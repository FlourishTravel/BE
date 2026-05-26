package com.flourishtravel.domain.planner.dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class PlannerSaveRequest {
    private String title;
    private PlannerGenerateRequest request;
    private List<PlannerDayDto> days;
    private PlannerBudgetDto budget;
    private UUID sessionId;
}
