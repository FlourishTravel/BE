package com.flourishtravel.domain.destination.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ThaiFestivalDto {
    private UUID id;
    private String slug;
    private String name;
    private String monthLabel;
    private String description;
    private String imageUrl;
}
