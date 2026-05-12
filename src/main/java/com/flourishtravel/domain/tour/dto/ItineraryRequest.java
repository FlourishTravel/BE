package com.flourishtravel.domain.tour.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** Payload 1 ngày lịch trình (cho bulk save). */
@Data
public class ItineraryRequest {

    @NotNull
    @Min(value = 1, message = "dayNumber phải >= 1")
    private Integer dayNumber;

    @NotBlank(message = "Tiêu đề ngày không được để trống")
    @Size(max = 255)
    private String title;

    private String description;

    private String summary;

    @Size(max = 500)
    private String coverImageUrl;

    @Size(max = 255)
    private String accommodation;

    @Size(max = 255)
    private String transport;

    /** CSV: BREAKFAST,LUNCH,DINNER (subset). */
    @Size(max = 100)
    private String mealsIncluded;

    private String highlights;

    @Valid
    private List<ActivityRequest> activities;
}
