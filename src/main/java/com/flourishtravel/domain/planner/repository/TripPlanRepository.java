package com.flourishtravel.domain.planner.repository;

import com.flourishtravel.domain.planner.entity.TripPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TripPlanRepository extends JpaRepository<TripPlan, UUID> {

    List<TripPlan> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
