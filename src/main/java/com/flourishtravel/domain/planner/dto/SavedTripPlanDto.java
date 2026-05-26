package com.flourishtravel.domain.planner.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class SavedTripPlanDto {
    private UUID id;
    private String title;
    private Instant createdAt;
    private String tripSummary;
}
