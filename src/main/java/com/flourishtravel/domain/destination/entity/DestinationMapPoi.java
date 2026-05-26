package com.flourishtravel.domain.destination.entity;

import com.flourishtravel.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "destination_map_pois")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DestinationMapPoi extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_id", nullable = false)
    private Destination destination;

    /** hotel | restaurant | attraction | transport | shopping */
    @Column(nullable = false, length = 40)
    private String category;

    /** budget | mid | luxury — chỉ áp dụng cho hotel */
    @Column(length = 20)
    private String tier;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(precision = 3, scale = 2)
    private BigDecimal rating;

    @Column(name = "price_label", length = 120)
    private String priceLabel;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    private Double latitude;
    private Double longitude;

    @Column(name = "sort_order")
    private Integer sortOrder;
}
