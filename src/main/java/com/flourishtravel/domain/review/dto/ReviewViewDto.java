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
    /** Mã đặt chỗ khách thấy (FT-XXXXXXXX). Chỉ trả cho chủ đơn / admin. */
    private String bookingCode;
    private UUID userId;
    private String userName;
    private UUID tourId;
    private String tourTitle;
    private Integer rating;
    private String comment;
    private String feedbackTags;
    private UUID guideId;
    private String guideName;
    private Integer guideRating;
    private String guideFeedbackTags;
    private Boolean isPublished;
    private Boolean isFeatured;
    private Instant createdAt;
}
