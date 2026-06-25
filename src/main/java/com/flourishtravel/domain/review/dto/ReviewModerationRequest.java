package com.flourishtravel.domain.review.dto;

import lombok.Data;

@Data
public class ReviewModerationRequest {
    private Boolean isPublished;
    private Boolean isFeatured;
}
