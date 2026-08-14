package com.flourishtravel.domain.tour.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Một đợt khởi hành khi tạo tour ({@code POST /tours}) hoặc thêm lịch sau
 * ({@code POST /admin/sessions}). {@code endDate} tuỳ chọn: nếu trống, BE tính
 * từ {@code durationDays} của tour.
 */
@Data
public class SessionRequest {

    @NotNull(message = "Ngày khởi hành là bắt buộc")
    private LocalDate startDate;

    private LocalDate endDate;

    @Min(value = 1, message = "Số khách tối đa phải >= 1")
    private Integer maxParticipants;

    private UUID tourGuideId;
}
