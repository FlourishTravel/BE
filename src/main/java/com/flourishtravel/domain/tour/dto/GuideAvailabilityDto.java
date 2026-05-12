package com.flourishtravel.domain.tour.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Hồ sơ HDV để admin chọn khi điều phối session.
 *
 * Trường mới hữu ích:
 *  - assignedThisMonth: số session đang phụ trách trong tháng (load balancing)
 *  - busyOnTargetDate : true nếu HDV đã có session trùng ngày/khoảng được hỏi
 *  - workloadLevel    : light | balanced | heavy (suy luận theo assignedThisMonth)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuideAvailabilityDto {

    private UUID id;
    private String fullName;
    private String initials;
    private String email;
    private String phone;
    private String avatarUrl;
    private boolean active;

    private int assignedThisMonth;
    private boolean busyOnTargetDate;
    private String workloadLevel;
}
