package com.flourishtravel.domain.guide.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class GuideSessionDetailDto {
    private UUID sessionId;
    private UUID tourId;
    private String tourTitle;
    private String tourCode;
    private String tourDescription;
    private String thumbnailUrl;
    private String location;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private int currentParticipants;
    private int maxParticipants;
    private int checkedInParticipants;
    private List<ItineraryDayDto> itineraryDays;

    @Data
    @Builder
    public static class ItineraryDayDto {
        private Integer dayNumber;
        private String title;
        private String summary;
        private String description;
        /** Các hoạt động trong ngày (theo sortOrder). */
        private List<ItineraryActivityDto> activities;
    }

    @Data
    @Builder
    public static class ItineraryActivityDto {
        private Integer sortOrder;
        private LocalTime startTime;
        private LocalTime endTime;
        private Integer durationMinutes;
        private String title;
        private String description;
        /** SIGHTSEEING | DINING | TRANSPORT | EXPERIENCE | FREE_TIME | SHOPPING | ACCOMMODATION */
        private String activityType;
        private String locationName;
        private String imageUrl;
    }
}
