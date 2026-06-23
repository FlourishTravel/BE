package com.flourishtravel.domain.tour.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionScheduleViewDto {

    private UUID sessionId;
    private UUID tourId;
    private List<SessionScheduleDayDto> days;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionScheduleDayDto {
        private Integer dayNumber;
        private String title;
        private List<SessionScheduleActivityDto> activities;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionScheduleActivityDto {
        private UUID activityId;
        private TemplateActivityDto template;
        private OverrideActivityDto override;
        private EffectiveActivityDto effective;
        private String sourceLabel;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateActivityDto {
        private String title;
        private LocalTime startTime;
        private LocalTime endTime;
        private String locationName;
        private String locationAddress;
        private Boolean isGatheringEvent;
        private String gatheringEventType;
        private String scheduleStatus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OverrideActivityDto {
        private String publicationStatus;
        private String title;
        private LocalTime startTime;
        private LocalTime endTime;
        private String locationName;
        private String locationAddress;
        private Boolean isGatheringEvent;
        private String gatheringEventType;
        private String scheduleStatus;
        private String operationalNote;
        private Integer version;
        private Instant publishedAt;
        private String publishedByName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EffectiveActivityDto {
        private String title;
        private LocalTime startTime;
        private LocalTime endTime;
        private String locationName;
        private String locationAddress;
        private String scheduleStatus;
        private Boolean isGatheringEvent;
        private String gatheringEventType;
        private boolean cancelled;
    }
}
