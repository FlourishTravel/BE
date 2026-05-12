package com.flourishtravel.domain.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload đổi trạng thái booking từ admin.
 * Các giá trị status được phép tuân theo flow:
 *   pending → paid → confirmed → completed
 *           ↘ cancelled
 *           ↘ refund_pending → refunded
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingStatusUpdateRequest {

    @NotBlank
    @Pattern(regexp = "^(pending|paid|confirmed|completed|cancelled)$",
            message = "status không hợp lệ")
    private String status;

    /** Ghi chú nội bộ (lý do, ghi chú admin). Hiện ghi log; có thể mở rộng lưu BookingActivity. */
    private String note;
}
