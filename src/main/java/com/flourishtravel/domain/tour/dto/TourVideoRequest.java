package com.flourishtravel.domain.tour.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TourVideoRequest {

    @Size(max = 500)
    private String videoUrl;

    @Size(max = 500)
    private String thumbnailUrl;

    @Size(max = 255)
    private String title;

    private Integer durationSeconds;
}
