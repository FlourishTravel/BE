package com.flourishtravel.domain.booking.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class GuestInputDto {
    private String fullName;
    private String idNumber;
    private LocalDate dateOfBirth;
}
