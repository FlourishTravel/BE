package com.flourishtravel.domain.tour.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GeocodeResultDto {

    private double latitude;
    private double longitude;
    private String label;
    private String provider;
}
