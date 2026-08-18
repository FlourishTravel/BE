package com.flourishtravel.domain.flora.repository;

import com.flourishtravel.domain.flora.entity.UserLocationPing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserLocationPingRepository extends JpaRepository<UserLocationPing, UUID> {

    Optional<UserLocationPing> findTopByBookingIdAndUserIdOrderByCapturedAtDesc(UUID bookingId, UUID userId);

    @Query("""
            SELECT p FROM UserLocationPing p
            JOIN FETCH p.user
            JOIN FETCH p.booking b
            WHERE b.session.id = :sessionId
            """)
    List<UserLocationPing> findBySessionId(@Param("sessionId") UUID sessionId);

    @Modifying
    @Query("DELETE FROM UserLocationPing p WHERE p.user.id = :userId")
    void deleteAllByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM UserLocationPing p WHERE p.capturedAt < :before")
    int deleteByCapturedAtBefore(@Param("before") Instant before);
}
