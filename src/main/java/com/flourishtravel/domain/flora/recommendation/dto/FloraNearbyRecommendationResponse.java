package com.flourishtravel.domain.flora.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FloraNearbyRecommendationResponse {

    private UUID bookingId;
    private String locationSource;
    private String locationLabel;
    private JourneyContextDto journeyContext;
    private List<RecommendationItemDto> recommendations;
    private List<String> warnings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JourneyContextDto {
        private String currentActivityTitle;
        private Instant nextMeetingTime;
        private String nextMeetingLocation;
        private String scheduleStatus;
        private Long freeMinutesUntilMeeting;
        private Boolean canValidateSchedule;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecommendationItemDto {
        private String id;
        private String name;
        private String category;
        private String address;
        private Double latitude;
        private Double longitude;
        private String dataSource;
        private Integer straightLineDistanceMeters;
        private Integer estimatedVisitMinutes;
        private Integer estimatedRoundTripMinutes;
        private Boolean fitsSchedule;
        private String foodMatchStatus;
        private String budgetMatchStatus;
        private String reason;
        private List<String> warnings;
        private MapActionDto mapAction;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MapActionDto {
        private String type;
        private Double latitude;
        private Double longitude;
    }
}
