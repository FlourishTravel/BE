package com.flourishtravel.domain.flora.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FloraActivityDto {

    private UUID id;
    private String title;
    private String description;
    private Instant startAt;
    private Instant endAt;
    private String locationName;
    private String locationAddress;
    private Double latitude;
    private Double longitude;
    private String activityType;
    private String scheduleStatus;
    private Integer dayNumber;

    /** Phase 1.5 — session override metadata (optional). */
    private String scheduleSource;
    private Integer scheduleVersion;
    private Instant lastUpdatedAt;
    private String lastUpdatedReason;
}
