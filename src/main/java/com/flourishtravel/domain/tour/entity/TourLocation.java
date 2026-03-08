package com.flourishtravel.domain.tour.entity;

import com.flourishtravel.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "tour_locations", indexes = @Index(columnList = "tour_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TourLocation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id", nullable = false)
    private Tour tour;

    @Column(name = "location_name", nullable = false, length = 255)
    private String locationName;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "google_place_id", length = 255)
    private String googlePlaceId;

    @Column(name = "visit_order", nullable = false)
    private Integer visitOrder;

    @Column(name = "day_number", nullable = false)
    private Integer dayNumber;
}
