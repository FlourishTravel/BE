package com.flourishtravel.domain.catalog.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CatalogPageDto {
    private List<TourCardDto> featuredTours;
    private List<TourCardDto> tours;
    private List<TicketCardDto> tickets;
    private FloraTourRecommendDto floraSuggestion;
}
