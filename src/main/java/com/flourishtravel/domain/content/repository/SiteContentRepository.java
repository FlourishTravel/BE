package com.flourishtravel.domain.content.repository;

import com.flourishtravel.domain.content.entity.SiteContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SiteContentRepository extends JpaRepository<SiteContent, UUID> {

    Optional<SiteContent> findBySlug(String slug);

    Optional<SiteContent> findBySlugAndPublishedTrue(String slug);

    boolean existsBySlugIgnoreCase(String slug);

    @Query("""
            SELECT c FROM SiteContent c
            WHERE c.published = true
              AND (:type IS NULL OR c.type = :type)
            ORDER BY c.sortOrder ASC, c.publishedAt DESC, c.createdAt DESC
            """)
    List<SiteContent> findPublic(@Param("type") String type);

    @Query("""
            SELECT c FROM SiteContent c
            WHERE (:type IS NULL OR c.type = :type)
            ORDER BY c.sortOrder ASC, c.createdAt DESC
            """)
    List<SiteContent> findAdmin(@Param("type") String type);
}
