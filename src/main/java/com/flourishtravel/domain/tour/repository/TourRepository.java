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

    @Query(
        value = """
        SELECT DISTINCT t.id, t.base_price, t.category_id, t.created_at, t.description,
               t.duration_days, t.duration_nights, t.slug, t.title, t.updated_at
        FROM tours t
        INNER JOIN tour_sessions s ON s.tour_id = t.id
        WHERE (CAST(:destinationPattern AS text) IS NULL OR LOWER(t.title) LIKE LOWER(CAST(:destinationPattern AS text)))
          AND (:minPrice IS NULL OR t.base_price >= :minPrice)
          AND (:maxPrice IS NULL OR t.base_price <= :maxPrice)
          AND (:startDate IS NULL OR s.start_date >= :startDate)
          AND (:categoryId IS NULL OR t.category_id = :categoryId)
          AND s.status = 'scheduled'
          AND s.current_participants < s.max_participants
        ORDER BY t.created_at ASC
        """,
        countQuery = """
        SELECT COUNT(DISTINCT t.id)
        FROM tours t
        INNER JOIN tour_sessions s ON s.tour_id = t.id
        WHERE (CAST(:destinationPattern AS text) IS NULL OR LOWER(t.title) LIKE LOWER(CAST(:destinationPattern AS text)))
          AND (:minPrice IS NULL OR t.base_price >= :minPrice)
          AND (:maxPrice IS NULL OR t.base_price <= :maxPrice)
          AND (:startDate IS NULL OR s.start_date >= :startDate)
          AND (:categoryId IS NULL OR t.category_id = :categoryId)
          AND s.status = 'scheduled'
          AND s.current_participants < s.max_participants
        """,
        nativeQuery = true
    )
    Page<Tour> search(@Param("destinationPattern") String destinationPattern,
                      @Param("minPrice") BigDecimal minPrice,
                      @Param("maxPrice") BigDecimal maxPrice,
                      @Param("startDate") LocalDate startDate,
                      @Param("categoryId") UUID categoryId,
                      Pageable pageable);

    /**
     * Tìm tour theo địa điểm/giá (không lọc session). Dùng cho chatbot gợi ý khi có thể chưa có session scheduled.
     */
    @Query(
        value = """
        SELECT t.id, t.base_price, t.category_id, t.created_at, t.description,
               t.duration_days, t.duration_nights, t.slug, t.title, t.updated_at
        FROM tours t
        WHERE (CAST(:destinationPattern AS text) IS NULL OR LOWER(t.title) LIKE LOWER(CAST(:destinationPattern AS text)))
          AND (:minPrice IS NULL OR t.base_price >= :minPrice)
          AND (:maxPrice IS NULL OR t.base_price <= :maxPrice)
          AND (:categoryId IS NULL OR t.category_id = :categoryId)
        ORDER BY t.created_at ASC
        """,
        countQuery = """
        SELECT COUNT(t.id) FROM tours t
        WHERE (CAST(:destinationPattern AS text) IS NULL OR LOWER(t.title) LIKE LOWER(CAST(:destinationPattern AS text)))
          AND (:minPrice IS NULL OR t.base_price >= :minPrice)
          AND (:maxPrice IS NULL OR t.base_price <= :maxPrice)
          AND (:categoryId IS NULL OR t.category_id = :categoryId)
        """,
        nativeQuery = true
    )
    Page<Tour> searchForSuggestion(@Param("destinationPattern") String destinationPattern,
                                   @Param("minPrice") BigDecimal minPrice,
                                   @Param("maxPrice") BigDecimal maxPrice,
                                   @Param("categoryId") UUID categoryId,
                                   Pageable pageable);

    Page<Tour> findByCategory_IdAndIdNotOrderByCreatedAtDesc(UUID categoryId, UUID excludeId, Pageable pageable);

    Page<Tour> findByIdNotOrderByCreatedAtDesc(UUID excludeId, Pageable pageable);
}
