package com.flourishtravel.domain.booking.repository;

import com.flourishtravel.domain.booking.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, UUID> {

    Optional<Promotion> findByCodeAndIsActiveTrue(String code);

    Optional<Promotion> findByCodeAndIsActiveTrueAndValidFromBeforeAndValidToAfter(
            String code, Instant now, Instant now2);
}
