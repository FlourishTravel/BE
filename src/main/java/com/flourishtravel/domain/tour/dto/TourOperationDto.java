package com.flourishtravel.domain.tour.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Payload cho trang Điều hành tour (Tour Operations / Dispatch).
 * Mỗi item = 1 session + tour + guide + occupancy + cờ "issue" để admin theo dõi.
 *
 * Trường mới phù hợp xu hướng hiện đại:
 *  - occupancyPercent: lấp đầy (%) — giúp admin nhanh chóng đánh giá tình trạng tour
 *  - hasGuideIssue   : cờ cảnh báo (chưa có HDV / HDV bị deactivate)
 *  - urgent          : cần điều phối khẩn cấp (start trong &lt;= 3 ngày &amp; chưa có guide)
 *  - bookingCount    : số booking đang gắn với session (đếm nhanh từ currentParticipants)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TourOperationDto {

    private UUID sessionId;
    private UUID tourId;
    private String tourTitle;
    private String tourSlug;
    private String tourCode;            // viết tắt (slug-uppercase / mã ngắn cho hiển thị)

    private LocalDate startDate;
    private LocalDate endDate;
    private String status;              // scheduled | cancelled | completed | full
    private String issueLevel;          // none | warning | critical

    private Integer maxParticipants;
    private Integer currentParticipants;
    private Integer remainingSlots;
    private Double occupancyPercent;    // 0..100

    private boolean hasGuideIssue;      // chưa có guide hoặc guide bị disable
    private boolean urgent;             // start trong 3 ngày tới & chưa có guide

    private GuideRef tourGuide;
    private String thumbnailUrl;
    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GuideRef {
        private UUID id;
        private String fullName;
        private String initials;
        private String email;
        private String phone;
        private String avatarUrl;
        private boolean active;
    }
}
