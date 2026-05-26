package com.flourishtravel.domain.destination.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MapStatsDto {
    private long hotels;
    private long restaurants;
    private long attractions;
}
