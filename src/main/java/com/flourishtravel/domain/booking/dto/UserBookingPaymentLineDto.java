package com.flourishtravel.domain.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBookingPaymentLineDto {

    private UUID paymentId;
    private String orderId;
    private BigDecimal amount;
    private String status;
    private String provider;
    private Instant createdAt;
}
