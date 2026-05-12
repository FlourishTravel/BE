package com.flourishtravel.domain.tour.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
public class ActivityRequest {

    @Min(value = 0)
    private Integer sortOrder;

    private LocalTime startTime;

    private LocalTime endTime;

    @Min(value = 0)
    private Integer durationMinutes;

    @Size(max = 255)
    private String title;

    private String description;

    /** SIGHTSEEING | DINING | TRANSPORT | EXPERIENCE | FREE_TIME | SHOPPING | ACCOMMODATION */
    @Pattern(
            regexp = "^$|^(SIGHTSEEING|DINING|TRANSPORT|EXPERIENCE|FREE_TIME|SHOPPING|ACCOMMODATION)$",
            message = "activityType không hợp lệ"
    )
    @Size(max = 30)
    private String activityType;

    @Size(max = 255)
    private String locationName;

    private BigDecimal latitude;

    private BigDecimal longitude;

    @Size(max = 500)
    private String imageUrl;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal costEstimate;

    private Boolean costIncluded;

    @Size(max = 500)
    private String tags;
}
