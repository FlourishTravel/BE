package com.flourishtravel.domain.destination.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class DestinationDetailDto {
    private UUID id;
    private String slug;
    private String name;
    private String summary;
    private String description;
    private String heroImageUrl;
    private String videoUrl;
    private BigDecimal rating;
    private Integer avgCostMinMillion;
    private Integer avgCostMaxMillion;
    private Integer avgTemperatureC;
    private Integer idealDaysMin;
    private Integer idealDaysMax;
    private String bestTimeLabel;
    private String locationLabel;
    private String timezone;
    private String language;
    private String currency;
    private String weatherNow;
    private String weatherForecast;
    private Double latitude;
    private Double longitude;
    private List<String> types;
    private List<String> highlightSpots;
    private List<AttractionDto> attractions;
    private List<CostItemDto> costItems;
    private List<MapPoiDto> mapPois;
    private List<ReviewDto> reviews;
    private List<TourSuggestionDto> suggestedTours;
    private FloraMatchDto floraSuggestion;

    @Data
    @Builder
    public static class AttractionDto {
        private UUID id;
        private String name;
        private String description;
        private String imageUrl;
        private String ticketPriceLabel;
        private String openHours;
        private Double latitude;
        private Double longitude;
    }

    @Data
    @Builder
    public static class CostItemDto {
        private String category;
        private String label;
        private Integer costMinMillion;
        private Integer costMaxMillion;
    }

    @Data
    @Builder
    public static class MapPoiDto {
        private UUID id;
        private String category;
        /** budget | mid | luxury */
        private String tier;
        private String name;
        private BigDecimal rating;
        private String priceLabel;
        private String imageUrl;
        private Double latitude;
        private Double longitude;
    }

    @Data
    @Builder
    public static class ReviewDto {
        private UUID id;
        private String authorName;
        private BigDecimal rating;
        private String comment;
    }

    @Data
    @Builder
    public static class TourSuggestionDto {
        private UUID id;
        private String title;
        private String slug;
        private String thumbnailUrl;
        private String durationLabel;
        private java.math.BigDecimal basePrice;
    }

    @Data
    @Builder
    public static class FloraMatchDto {
        private String destinationSlug;
        private String destinationName;
        private int matchPercent;
        private List<String> matchedPreferences;
        private String message;
    }
}
