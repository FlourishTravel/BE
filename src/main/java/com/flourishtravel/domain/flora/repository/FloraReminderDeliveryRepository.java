package com.flourishtravel.domain.flora.repository;

import com.flourishtravel.domain.flora.entity.FloraReminderDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FloraReminderDeliveryRepository extends JpaRepository<FloraReminderDelivery, UUID> {

    boolean existsByIdempotencyKey(String idempotencyKey);

    Optional<FloraReminderDelivery> findByIdempotencyKey(String idempotencyKey);
}
