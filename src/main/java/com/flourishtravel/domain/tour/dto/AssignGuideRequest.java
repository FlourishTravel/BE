package com.flourishtravel.domain.tour.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Payload khi admin gán / đổi HDV cho 1 session.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignGuideRequest {

    @NotNull(message = "guideId không được để trống")
    private UUID guideId;

    /** Có gửi email thông báo cho HDV mới (và HDV cũ nếu thay) không. */
    private Boolean notify;

    /** Ghi chú nội bộ (tuỳ chọn). */
    private String note;
}
