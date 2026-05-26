package com.flourishtravel.domain.catalog.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class FloraTourRecommendDto {
    private String message;
    private Long budgetVnd;
    private UUID tourId;
    private String tourTitle;
    private String tourSlug;
    private String durationLabel;
    private BigDecimal priceVnd;
    private int matchPercent;
}
