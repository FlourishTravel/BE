package com.flourishtravel.domain.content.entity;

import com.flourishtravel.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "site_contents", indexes = {
        @Index(name = "idx_site_content_type", columnList = "type"),
        @Index(name = "idx_site_content_slug", columnList = "slug", unique = true),
        @Index(name = "idx_site_content_published", columnList = "published")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiteContent extends BaseEntity {

    @Column(nullable = false, length = 20)
    private String type;

    @Column(nullable = false, unique = true, length = 200)
    private String slug;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 500)
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(length = 120)
    private String category;

    @Column(nullable = false)
    @Builder.Default
    private Boolean published = false;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "published_at")
    private Instant publishedAt;
}
