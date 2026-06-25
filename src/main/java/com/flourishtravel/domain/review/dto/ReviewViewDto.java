package com.flourishtravel.domain.review.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ReviewViewDto {
    private UUID id;
    private UUID bookingId;
    private UUID userId;
    private String userName;
    private UUID tourId;
    private String tourTitle;
    private Integer rating;
    private String comment;
    private String feedbackTags;
    private Boolean isPublished;
    private Boolean isFeatured;
    private Instant createdAt;
}
