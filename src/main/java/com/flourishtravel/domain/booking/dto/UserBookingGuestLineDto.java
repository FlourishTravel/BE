package com.flourishtravel.domain.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBookingGuestLineDto {

    private UUID guestId;
    private String fullName;
    /** CCCD đã che (ví dụ ***1234). */
    private String maskedIdNumber;
    private LocalDate dateOfBirth;
    private Integer sortOrder;
}
