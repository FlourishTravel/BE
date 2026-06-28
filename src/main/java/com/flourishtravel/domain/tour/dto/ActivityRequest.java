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

    @Size(max = 2000)
    private String imageUrl;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal costEstimate;

    private Boolean costIncluded;

    @Size(max = 500)
    private String tags;

    @Size(max = 2000)
    private String locationAddress;

    private Boolean isGatheringEvent;

    @Pattern(
            regexp = "^$|^(DEPARTURE|MEETING|RETURN_TO_BUS|CHECK_IN|CHECK_OUT)$",
            message = "gatheringEventType không hợp lệ"
    )
    @Size(max = 30)
    private String gatheringEventType;

    @Pattern(
            regexp = "^$|^(CONFIRMED|ESTIMATED|UNAVAILABLE)$",
            message = "scheduleStatus phải là CONFIRMED, ESTIMATED hoặc UNAVAILABLE"
    )
    @Size(max = 20)
    private String scheduleStatus;
}
