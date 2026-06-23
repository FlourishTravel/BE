package com.flourishtravel.domain.tour.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Payload chi tiết 1 tour cho admin (bao gồm category, ảnh, video, lịch trình, địa điểm, session).
 * Dùng riêng (không bỏ @JsonIgnore trên entity) để tránh ảnh hưởng API public hiện có.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TourDetailDto {

    private UUID id;
    private String title;
    private String slug;
    private String description;
    private BigDecimal basePrice;
    private Integer durationDays;
    private Integer durationNights;

    private TourSummaryDto.CategoryRef category;

    /** Trạng thái suy luận: draft | active | upcoming | full */
    private String status;

    private List<ImageRef> images;
    private List<VideoRef> videos;
    private List<ItineraryRef> itineraries;
    private List<LocationRef> locations;
    private List<SessionDetail> sessions;

    private String destinationCity;
    private BigDecimal rating;
    private String badge;
    private List<String> tags;
    private List<String> highlights;
    private List<String> includes;
    private List<String> excludes;
    private List<ReviewRef> reviews;
    private String thumbnailUrl;

    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewRef {
        private String authorName;
        private BigDecimal rating;
        private String comment;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageRef {
        private UUID id;
        private String imageUrl;
        private String caption;
        private Integer sortOrder;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VideoRef {
        private UUID id;
        private String videoUrl;
        private String thumbnailUrl;
        private String title;
        private Integer durationSeconds;
        private Integer sortOrder;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItineraryRef {
        private UUID id;
        private Integer dayNumber;
        private String title;
        private String description;

        /** Các trường mới (xu hướng modern itinerary). */
        private String summary;
        private String coverImageUrl;
        private String accommodation;
        private String transport;
        private String mealsIncluded;
        private String highlights;

        private List<ActivityRef> activities;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityRef {
        private UUID id;
        private Integer sortOrder;
        private java.time.LocalTime startTime;
        private java.time.LocalTime endTime;
        private Integer durationMinutes;
        private String title;
        private String description;
        private String activityType;
        private String locationName;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private String imageUrl;
        private BigDecimal costEstimate;
        private Boolean costIncluded;
        private String tags;
        private String locationAddress;
        private Boolean isGatheringEvent;
        private String gatheringEventType;
        private String scheduleStatus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationRef {
        private UUID id;
        private String locationName;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private Integer visitOrder;
        private Integer dayNumber;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionDetail {
        private UUID id;
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer maxParticipants;
        private Integer currentParticipants;
        private String status;
        private GuideRef tourGuide;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class GuideRef {
        private UUID id;
        private String fullName;
        private String email;
        private String avatarUrl;
    }
}
