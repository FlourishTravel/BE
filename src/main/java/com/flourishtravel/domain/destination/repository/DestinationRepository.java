package com.flourishtravel.domain.destination.repository;

import com.flourishtravel.domain.destination.entity.Destination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DestinationRepository extends JpaRepository<Destination, UUID> {

    Optional<Destination> findBySlugAndPublishedTrue(String slug);

    boolean existsBySlugIgnoreCase(String slug);

    @Query("""
            SELECT d FROM Destination d
            WHERE d.published = true
            AND (:type IS NULL OR :type = '' OR LOWER(d.types) LIKE LOWER(CONCAT('%', :type, '%')))
            AND (:q IS NULL OR :q = '' OR LOWER(d.name) LIKE LOWER(CONCAT('%', :q, '%'))
                 OR LOWER(d.summary) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY d.sortOrder ASC, d.name ASC
            """)
    List<Destination> findPublished(@Param("type") String type, @Param("q") String q);

    @Query("SELECT d FROM Destination d WHERE d.published = true AND d.featured = true ORDER BY d.sortOrder ASC")
    List<Destination> findFeatured();
}
