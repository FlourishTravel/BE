package com.flourishtravel.domain.user.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class PublicGuideSummaryDto {
    private UUID id;
    private String fullName;
    private String avatarUrl;
    private String coverImageUrl;
    private String jobTitle;
    private String location;
    private String shortBio;
    private String bio;
    private List<String> languages;
    private List<String> specialties;
    private List<String> badges;
    private Boolean verified;
    private Integer experienceYears;
    private BigDecimal rating;
    private long reviewCount;
    private long toursCompleted;
    private Instant joinedAt;
    private List<AssignedTourRef> tours;
    private List<GuideReviewRef> reviews;

    @Data
    @Builder
    public static class AssignedTourRef {
        private UUID id;
        private String title;
        private Integer durationDays;
        private Integer durationNights;
        private BigDecimal basePrice;
        private String thumbnailUrl;
        private LocalDate nextStartDate;
    }

    @Data
    @Builder
    public static class GuideReviewRef {
        private UUID id;
        private String authorName;
        private String authorAvatarUrl;
        private Integer rating;
        private String comment;
        private String tourName;
        private Instant createdAt;
    }
}
