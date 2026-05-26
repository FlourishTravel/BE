package com.flourishtravel.domain.catalog.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class TourCardDto {
    private UUID id;
    private String title;
    private String slug;
    private String description;
    private BigDecimal basePrice;
    private String priceFromLabel;
    private Integer durationDays;
    private Integer durationNights;
    private String durationLabel;
    private String thumbnailUrl;
    private String destinationCity;
    private String locationLabel;
    private BigDecimal rating;
    private String badge;
    private List<String> tags;
    private Boolean featured;
    private String status;
}
