package com.flourishtravel.domain.tour.entity;

import com.flourishtravel.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "categories", indexes = @Index(columnList = "slug", unique = true))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(unique = true, nullable = false, length = 100)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "sort_order")
    private Integer sortOrder;

    /**
     * Soft-delete marker. NULL = đang hoạt động; khác NULL = đã lưu trữ.
     * Giữ FK với tour không bị vỡ và admin có thể khôi phục bất kỳ lúc nào.
     */
    @Column(name = "deleted_at")
    private Instant deletedAt;
}
