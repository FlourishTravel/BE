package com.flourishtravel.domain.flora.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class FloraLocationRequest {

    private Double latitude;
    private Double longitude;
    private Double accuracyMeters;
    private Instant capturedAt;
}
