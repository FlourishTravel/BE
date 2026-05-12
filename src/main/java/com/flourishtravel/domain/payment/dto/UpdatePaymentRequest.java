package com.flourishtravel.domain.payment.dto;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Payload cho admin cập nhật 1 payment record.
 * Dùng cho: thêm ghi chú, đánh dấu fail/paid thủ công, cập nhật fee.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePaymentRequest {

    /** pending | paid | failed | refunded — optional. */
    @Pattern(regexp = "^(pending|paid|failed|refunded)?$",
            message = "status phải là pending, paid, failed hoặc refunded")
    private String status;

    private BigDecimal feeAmount;

    private String failureReason;

    private String adminNote;
}
