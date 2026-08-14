package com.flourishtravel.domain.booking.repository;

import com.flourishtravel.domain.booking.entity.PromotionAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PromotionAssignmentRepository extends JpaRepository<PromotionAssignment, UUID> {

    boolean existsByPromotion_IdAndUser_Id(UUID promotionId, UUID userId);

    Optional<PromotionAssignment> findByPromotion_IdAndUser_Id(UUID promotionId, UUID userId);

    long countByPromotion_Id(UUID promotionId);

    @Query("""
            SELECT a FROM PromotionAssignment a
            JOIN FETCH a.user
            WHERE a.promotion.id = :promotionId
            ORDER BY a.createdAt DESC
            """)
    List<PromotionAssignment> findWithUserByPromotionId(@Param("promotionId") UUID promotionId);

    @Query("""
            SELECT a FROM PromotionAssignment a
            JOIN FETCH a.promotion
            WHERE a.user.id = :userId
            """)
    List<PromotionAssignment> findWithPromotionByUserId(@Param("userId") UUID userId);

    @Query("""
            SELECT a.promotion.id, COUNT(a)
            FROM PromotionAssignment a
            GROUP BY a.promotion.id
            """)
    List<Object[]> countGroupedByPromotionId();

    void deleteByPromotion_IdAndUser_Id(UUID promotionId, UUID userId);
}
