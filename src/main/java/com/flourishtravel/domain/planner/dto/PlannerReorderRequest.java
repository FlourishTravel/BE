package com.flourishtravel.domain.planner.dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class PlannerReorderRequest {
    private List<PlannerDayDto> days;
    private UUID sessionId;
}
