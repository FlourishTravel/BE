package com.flourishtravel.domain.flora.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FloraLocationResponse {

    private boolean accepted;
    private Double distanceToMeetingMeters;
    private Boolean returnToBusSuggested;
    private String message;
}
