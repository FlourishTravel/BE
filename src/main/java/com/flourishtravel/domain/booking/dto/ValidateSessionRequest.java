package com.flourishtravel.domain.booking.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ValidateSessionRequest {
    @NotNull
    private UUID sessionId;

    @NotNull
    @Min(1)
    private Integer guestCount;

    /** Khi gửi, BE xác nhận session thuộc đúng tour (chống gửi session tour khác). */
    private UUID tourId;
}
