package com.flourishtravel.domain.content.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class UpdateSiteContentRequest {
    private String type;
    private String slug;
    private String title;
    private String summary;
    private String body;
    private String imageUrl;
    private String category;
    private Boolean published;
    private Integer sortOrder;
    private Instant publishedAt;
}
