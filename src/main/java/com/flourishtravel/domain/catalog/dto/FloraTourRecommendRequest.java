package com.flourishtravel.domain.catalog.dto;

import lombok.Data;

@Data
public class FloraTourRecommendRequest {
    private Long budgetVnd;
    private String destination;
    private Integer guests;
}
