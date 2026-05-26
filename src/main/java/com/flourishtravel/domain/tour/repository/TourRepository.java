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
        SELECT DISTINCT t FROM Tour t
        INNER JOIN t.sessions s
        WHERE (LOWER(t.title) LIKE LOWER(:destinationPattern)
               OR LOWER(COALESCE(t.destinationCity, '')) LIKE LOWER(:destinationPattern))
          AND (:minPrice IS NULL OR t.basePrice >= :minPrice)
          AND (:maxPrice IS NULL OR t.basePrice <= :maxPrice)
          AND (:startDate IS NULL OR s.startDate >= :startDate)
          AND (:categoryId IS NULL OR t.category.id = :categoryId)
          AND s.status = 'scheduled'
          AND s.currentParticipants < s.maxParticipants
        ORDER BY t.createdAt ASC
        """,
        countQuery = """
        SELECT COUNT(DISTINCT t.id) FROM Tour t
        INNER JOIN t.sessions s
        WHERE (LOWER(t.title) LIKE LOWER(:destinationPattern)
               OR LOWER(COALESCE(t.destinationCity, '')) LIKE LOWER(:destinationPattern))
          AND (:minPrice IS NULL OR t.basePrice >= :minPrice)
          AND (:maxPrice IS NULL OR t.basePrice <= :maxPrice)
          AND (:startDate IS NULL OR s.startDate >= :startDate)
          AND (:categoryId IS NULL OR t.category.id = :categoryId)
          AND s.status = 'scheduled'
          AND s.currentParticipants < s.maxParticipants
        """
    )
    Page<Tour> search(@Param("destinationPattern") String destinationPattern,
                      @Param("minPrice") BigDecimal minPrice,
                      @Param("maxPrice") BigDecimal maxPrice,
                      @Param("startDate") LocalDate startDate,
                      @Param("categoryId") UUID categoryId,
                      Pageable pageable);

    /**
     * Tìm tour theo địa điểm/giá (không lọc session). Trang catalog / gợi ý / danh sách công khai.
     */
    @Query(
        value = """
        SELECT t FROM Tour t
        WHERE (LOWER(t.title) LIKE LOWER(:destinationPattern)
               OR LOWER(COALESCE(t.destinationCity, '')) LIKE LOWER(:destinationPattern))
          AND (:minPrice IS NULL OR t.basePrice >= :minPrice)
          AND (:maxPrice IS NULL OR t.basePrice <= :maxPrice)
          AND (:categoryId IS NULL OR t.category.id = :categoryId)
        ORDER BY t.featured DESC NULLS LAST, t.createdAt DESC
        """,
        countQuery = """
        SELECT COUNT(t.id) FROM Tour t
        WHERE (LOWER(t.title) LIKE LOWER(:destinationPattern)
               OR LOWER(COALESCE(t.destinationCity, '')) LIKE LOWER(:destinationPattern))
          AND (:minPrice IS NULL OR t.basePrice >= :minPrice)
          AND (:maxPrice IS NULL OR t.basePrice <= :maxPrice)
          AND (:categoryId IS NULL OR t.category.id = :categoryId)
        """
    )
    Page<Tour> searchForSuggestion(@Param("destinationPattern") String destinationPattern,
                                   @Param("minPrice") BigDecimal minPrice,
                                   @Param("maxPrice") BigDecimal maxPrice,
                                   @Param("categoryId") UUID categoryId,
                                   Pageable pageable);

    Page<Tour> findByCategory_IdAndIdNotOrderByCreatedAtDesc(UUID categoryId, UUID excludeId, Pageable pageable);

    Page<Tour> findByIdNotOrderByCreatedAtDesc(UUID excludeId, Pageable pageable);

    @Query("""
        SELECT t FROM Tour t
        WHERE LOWER(t.title) LIKE :pattern
           OR LOWER(t.slug)  LIKE :pattern
        ORDER BY t.createdAt DESC
        """)
    Page<Tour> adminSearch(@Param("pattern") String pattern, Pageable pageable);

    long countByCategory_Id(UUID categoryId);

    @Query("""
            SELECT DISTINCT t FROM Tour t
            LEFT JOIN FETCH t.itineraries it
            WHERE t.id = :id
            """)
    Optional<Tour> findByIdWithItinerariesAndActivities(@Param("id") UUID id);
}
