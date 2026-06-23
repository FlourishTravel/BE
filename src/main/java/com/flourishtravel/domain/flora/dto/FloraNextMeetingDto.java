package com.flourishtravel.domain.flora.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FloraNextMeetingDto {

    /** @deprecated use locationName — kept for backward compatibility */
    private Instant time;
    private String location;
    private Long minutesUntil;

    private String eventType;
    private String locationName;
    private String locationAddress;
    private Double latitude;
    private Double longitude;
    private String scheduleStatus;
    private Boolean reminderEligible;

    /** Phase 1.5 — session override metadata (optional). */
    private String scheduleSource;
    private Integer scheduleVersion;
    private Instant lastUpdatedAt;
    private String lastUpdatedReason;
}
