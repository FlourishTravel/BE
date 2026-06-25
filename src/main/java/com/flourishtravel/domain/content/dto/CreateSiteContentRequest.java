package com.flourishtravel.domain.content.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.Instant;

@Data
public class CreateSiteContentRequest {
    @NotBlank
    private String type;
    @NotBlank
    private String slug;
    @NotBlank
    private String title;
    private String summary;
    private String body;
    private String imageUrl;
    private String category;
    private Boolean published;
    private Integer sortOrder;
    private Instant publishedAt;
}
