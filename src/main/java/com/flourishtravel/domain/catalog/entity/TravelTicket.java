package com.flourishtravel.domain.catalog.entity;

import com.flourishtravel.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "travel_tickets", indexes = {
        @Index(columnList = "slug", unique = true),
        @Index(columnList = "category"),
        @Index(columnList = "featured")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelTicket extends BaseEntity {

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Column(nullable = false, length = 200)
    private String name;

    /** attraction | show | transport | flight | combo */
    @Column(nullable = false, length = 40)
    private String category;

    @Column(name = "destination_city", length = 80)
    private String destinationCity;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "short_description", length = 500)
    private String shortDescription;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "price_vnd", precision = 15, scale = 2)
    private BigDecimal priceVnd;

    @Column(name = "price_label", length = 80)
    private String priceLabel;

    @Column(precision = 3, scale = 2)
    private BigDecimal rating;

    @Column(name = "show_time_label", length = 120)
    private String showTimeLabel;

    @Column(name = "location_label", length = 200)
    private String locationLabel;

    @Column(name = "route_label", length = 200)
    private String routeLabel;

    @Builder.Default
    private Boolean eTicket = true;

    @Builder.Default
    private Boolean featured = false;

    @Builder.Default
    private Boolean published = true;

    @Column(name = "sort_order")
    private Integer sortOrder;
}
