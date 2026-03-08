package com.flourishtravel.domain.tour.repository;

import com.flourishtravel.domain.tour.entity.Tour;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TourRepository extends JpaRepository<Tour, UUID> {

    Optional<Tour> findBySlug(String slug);

    @Query("""
        SELECT DISTINCT t FROM Tour t
        JOIN t.sessions s
        WHERE (:destination IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :destination, '%')))
        AND (:minPrice IS NULL OR t.basePrice >= :minPrice)
        AND (:maxPrice IS NULL OR t.basePrice <= :maxPrice)
        AND (:startDate IS NULL OR s.startDate >= :startDate)
        AND (:categoryId IS NULL OR t.category.id = :categoryId)
        AND s.status = 'scheduled'
        AND s.currentParticipants < s.maxParticipants
        """)
    Page<Tour> search(@Param("destination") String destination,
                      @Param("minPrice") BigDecimal minPrice,
                      @Param("maxPrice") BigDecimal maxPrice,
                      @Param("startDate") LocalDate startDate,
                      @Param("categoryId") UUID categoryId,
                      Pageable pageable);

    Page<Tour> findByCategory_IdAndIdNotOrderByCreatedAtDesc(UUID categoryId, UUID excludeId, Pageable pageable);

    Page<Tour> findByIdNotOrderByCreatedAtDesc(UUID excludeId, Pageable pageable);
}
