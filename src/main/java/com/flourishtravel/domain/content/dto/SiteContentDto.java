package com.flourishtravel.domain.content.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class SiteContentDto {
    private UUID id;
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
