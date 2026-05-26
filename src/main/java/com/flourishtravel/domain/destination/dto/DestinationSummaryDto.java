package com.flourishtravel.domain.destination.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class DestinationSummaryDto {
    private UUID id;
    private String slug;
    private String name;
    private String summary;
    private String heroImageUrl;
    private BigDecimal rating;
    private Integer avgCostMinMillion;
    private Integer avgCostMaxMillion;
    private Integer avgTemperatureC;
    private Integer idealDaysMin;
    private Integer idealDaysMax;
    private String bestTimeLabel;
    private List<String> types;
    private List<String> highlightSpots;
}
