package com.flourishtravel.domain.destination.entity;

import com.flourishtravel.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "destination_cost_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DestinationCostItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_id", nullable = false)
    private Destination destination;

    @Column(nullable = false, length = 80)
    private String category;

    @Column(nullable = false, length = 120)
    private String label;

    @Column(name = "cost_min_million")
    private Integer costMinMillion;

    @Column(name = "cost_max_million")
    private Integer costMaxMillion;

    @Column(name = "sort_order")
    private Integer sortOrder;
}
