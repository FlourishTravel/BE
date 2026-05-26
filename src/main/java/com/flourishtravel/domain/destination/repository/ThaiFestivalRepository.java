package com.flourishtravel.domain.destination.repository;

import com.flourishtravel.domain.destination.entity.ThaiFestival;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ThaiFestivalRepository extends JpaRepository<ThaiFestival, UUID> {

    List<ThaiFestival> findByPublishedTrueOrderBySortOrderAsc();

    Optional<ThaiFestival> findBySlugAndPublishedTrue(String slug);
}
