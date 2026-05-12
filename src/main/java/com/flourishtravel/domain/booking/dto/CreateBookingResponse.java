package com.flourishtravel.domain.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Payload trả về sau POST /bookings — không nhúng entity Booking (tránh lazy/JSON nặng). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookingResponse {
    private UUID bookingId;
    private String orderId;
    private String paymentUrl;
    private int expiresInSeconds;
}
