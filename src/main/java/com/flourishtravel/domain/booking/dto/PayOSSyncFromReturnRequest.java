package com.flourishtravel.domain.booking.dto;

import lombok.Data;

@Data
public class PayOSSyncFromReturnRequest {
    /** Mã đơn FlourishTravel (vd. FT-xxxxxxxx) — ưu tiên nếu có. */
    private String orderId;
    /** Mã orderCode PayOS (query param khi redirect về returnUrl). */
    private String orderCode;
}
