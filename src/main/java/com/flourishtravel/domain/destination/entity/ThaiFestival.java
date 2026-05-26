package com.flourishtravel.domain.destination.entity;

import com.flourishtravel.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "thai_festivals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThaiFestival extends BaseEntity {

    @Column(nullable = false, unique = true, length = 80)
    private String slug;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "month_label", length = 80)
    private String monthLabel;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "long_description", columnDefinition = "TEXT")
    private String longDescription;

    @Column(name = "related_destination_slug", length = 80)
    private String relatedDestinationSlug;

    @Column(name = "video_url", length = 500)
    private String videoUrl;

    /** Mỗi dòng một mẹo / điểm cần biết */
    @Column(columnDefinition = "TEXT")
    private String tips;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Builder.Default
    private Boolean published = true;
}
