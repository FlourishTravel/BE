package com.flourishtravel.domain.destination.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminDestinationRequest {

    @NotBlank
    @Size(max = 80)
    private String slug;

    @NotBlank
    @Size(max = 120)
    private String name;

    private String summary;
    private String description;

    @Size(max = 500)
    private String heroImageUrl;

    /** beach,city,culture — CSV */
    @Size(max = 200)
    private String types;

    private BigDecimal rating;
    private Integer avgCostMinMillion;
    private Integer avgCostMaxMillion;
    private Integer idealDaysMin;
    private Integer idealDaysMax;

    @Size(max = 120)
    private String bestTimeLabel;

    @Size(max = 200)
    private String locationLabel;

    private Integer sortOrder;
    private Boolean featured;
    private Boolean published;
}
