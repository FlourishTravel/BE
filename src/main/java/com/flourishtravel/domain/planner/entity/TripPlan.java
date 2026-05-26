package com.flourishtravel.domain.planner.entity;

import com.flourishtravel.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "trip_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripPlan extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "trip_summary", length = 500)
    private String tripSummary;

    @Column(name = "request_json", columnDefinition = "TEXT")
    private String requestJson;

    @Column(name = "itinerary_json", columnDefinition = "TEXT")
    private String itineraryJson;

    @Column(name = "budget_json", columnDefinition = "TEXT")
    private String budgetJson;
}
