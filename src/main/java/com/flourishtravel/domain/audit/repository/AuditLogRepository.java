package com.flourishtravel.domain.audit.repository;

import com.flourishtravel.domain.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findByEntityTypeOrderByCreatedAtDesc(String entityType, Pageable pageable);

    Page<AuditLog> findByUser_IdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<AuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(Instant from, Instant to, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE (:entityType IS NULL OR a.entityType = :entityType) " +
            "AND (:userId IS NULL OR a.user.id = :userId) " +
            "AND (:from IS NULL OR a.createdAt >= :from) AND (:to IS NULL OR a.createdAt <= :to)")
    Page<AuditLog> findFiltered(@Param("entityType") String entityType, @Param("userId") UUID userId,
                                @Param("from") Instant from, @Param("to") Instant to, Pageable pageable);
}
