package com.flourishtravel.domain.destination.entity;

import com.flourishtravel.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "destinations", indexes = {
        @Index(columnList = "slug", unique = true),
        @Index(columnList = "featured")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Destination extends BaseEntity {

    @Column(nullable = false, unique = true, length = 80)
    private String slug;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "hero_image_url", length = 500)
    private String heroImageUrl;

    @Column(name = "video_url", length = 500)
    private String videoUrl;

    /** beach,city,culture,food,shopping,nightlife,family,couple,backpacker */
    @Column(length = 200)
    private String types;

    @Column(precision = 3, scale = 2)
    private BigDecimal rating;

    @Column(name = "avg_cost_min_million")
    private Integer avgCostMinMillion;

    @Column(name = "avg_cost_max_million")
    private Integer avgCostMaxMillion;

    @Column(name = "avg_temperature_c")
    private Integer avgTemperatureC;

    @Column(name = "ideal_days_min")
    private Integer idealDaysMin;

    @Column(name = "ideal_days_max")
    private Integer idealDaysMax;

    @Column(name = "best_time_label", length = 120)
    private String bestTimeLabel;

    @Column(name = "location_label", length = 200)
    private String locationLabel;

    @Column(length = 80)
    private String timezone;

    @Column(length = 80)
    private String language;

    @Column(length = 40)
    private String currency;

    @Column(name = "weather_now", length = 120)
    private String weatherNow;

    @Column(name = "weather_forecast", columnDefinition = "TEXT")
    private String weatherForecast;

    private Double latitude;
    private Double longitude;

    @Column(name = "flora_match_default")
    private Integer floraMatchDefault;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Builder.Default
    private Boolean featured = true;

    @Builder.Default
    private Boolean published = true;

    @OneToMany(mappedBy = "destination", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DestinationAttraction> attractions = new ArrayList<>();

    @OneToMany(mappedBy = "destination", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DestinationCostItem> costItems = new ArrayList<>();

    @OneToMany(mappedBy = "destination", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DestinationMapPoi> mapPois = new ArrayList<>();

    @OneToMany(mappedBy = "destination", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DestinationReview> reviews = new ArrayList<>();

    @OneToMany(mappedBy = "destination", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DestinationHighlightSpot> highlightSpots = new ArrayList<>();
}
