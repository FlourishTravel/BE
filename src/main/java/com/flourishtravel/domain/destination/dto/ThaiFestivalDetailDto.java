package com.flourishtravel.domain.destination.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ThaiFestivalDetailDto {
    private UUID id;
    private String slug;
    private String name;
    private String monthLabel;
    private String description;
    private String longDescription;
    private String imageUrl;
    private String videoUrl;
    private String relatedDestinationSlug;
    private String relatedDestinationName;
    private List<String> tips;
}
