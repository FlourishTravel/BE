package com.flourishtravel.domain.flora.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FloraJourneyDto {

    private UUID bookingId;
    private String bookingStatus;
    private String tourTitle;
    private String tourSlug;
    private UUID tourId;
    private LocalDate sessionStartDate;
    private LocalDate sessionEndDate;
    private Integer guestCount;
    private String meetingPoint;
    private Instant nextGatheringAt;
    private Long minutesUntilGathering;
    private FloraScheduleItemDto currentScheduleItem;
    private FloraScheduleItemDto nextScheduleItem;
    private String weatherSummary;
    private List<String> packingReminders;
    private List<String> importantNotices;
    private FloraNextMeetingDto nextMeeting;

    /** Phase 1.2 — activity-level journey (optional). */
    private String journeyStatus;
    private FloraActivityDto currentActivity;
    private FloraActivityDto nextActivity;
    private List<String> warnings;
    private Long freeMinutesUntilMeeting;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FloraScheduleItemDto {
        private Integer dayNumber;
        private String title;
        private String summary;
    }
}
