package com.flourishtravel.domain.booking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MomoSyncFromReturnRequest {
    /** Mã đơn hàng gửi lên MoMo (vd. FT-xxxxxxxx). */
    @NotBlank
    private String orderId;
}
