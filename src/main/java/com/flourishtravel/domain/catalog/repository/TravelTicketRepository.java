package com.flourishtravel.domain.catalog.repository;

import com.flourishtravel.domain.catalog.entity.TravelTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TravelTicketRepository extends JpaRepository<TravelTicket, UUID> {

    Optional<TravelTicket> findBySlugAndPublishedTrue(String slug);

    Optional<TravelTicket> findBySlug(String slug);

    boolean existsBySlugIgnoreCase(String slug);

    @Query("""
            SELECT t FROM TravelTicket t
            WHERE t.published = true
            AND (:category IS NULL OR :category = '' OR t.category = :category)
            AND (:destination IS NULL OR :destination = '' OR LOWER(t.destinationCity) LIKE LOWER(CONCAT('%', :destination, '%'))
                 OR LOWER(t.name) LIKE LOWER(CONCAT('%', :destination, '%')))
            ORDER BY t.featured DESC, t.sortOrder ASC, t.name ASC
            """)
    List<TravelTicket> search(@Param("category") String category, @Param("destination") String destination);

    List<TravelTicket> findByPublishedTrueAndFeaturedTrueOrderBySortOrderAsc();
}
