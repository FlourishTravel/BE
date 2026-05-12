package com.flourishtravel.domain.booking.dto;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Payload duyệt / từ chối refund từ admin.
 *
 *  - refundId: ID refund record (nếu bỏ trống lấy refund pending mới nhất của booking).
 *  - amount  : số tiền duyệt hoàn (cho phép hoàn 1 phần, mặc định = refund.amount).
 *  - reason  : ghi chú admin (mandatory khi reject).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundActionRequest {

    private UUID refundId;

    @PositiveOrZero
    private BigDecimal amount;

    private String reason;
}
