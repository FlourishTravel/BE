package com.flourishtravel.domain.tour.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Payload tóm tắt 1 tour cho danh sách (đặc biệt cho admin).
 * - Đính kèm category, ảnh đại diện và session sớm nhất để FE hiển thị
 *   mà không cần lazy-load các quan hệ @JsonIgnore trên Tour entity.
 * - `status` được suy ra phía BE (draft / active / upcoming / full).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TourSummaryDto {

    private UUID id;
    private String title;
    private String slug;
    private String description;
    private BigDecimal basePrice;
    private Integer durationDays;
    private Integer durationNights;

    private String thumbnailUrl;

    private CategoryRef category;

    private SessionRef earliestSession;
    private Integer sessionsCount;

    /** draft | active | upcoming | full */
    private String status;

    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryRef {
        private UUID id;
        private String name;
        private String slug;
        private boolean archived;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionRef {
        private UUID id;
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer maxParticipants;
        private Integer currentParticipants;
        private String status;
    }
}
