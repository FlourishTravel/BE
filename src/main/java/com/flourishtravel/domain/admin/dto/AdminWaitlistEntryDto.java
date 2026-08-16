package com.flourishtravel.domain.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class AdminWaitlistEntryDto {
    private UUID id;
    private UUID userId;
    private String fullName;
    private String email;
    private String phone;
    private UUID tourId;
    private UUID sessionId;
    private LocalDate sessionStartDate;
    private LocalDate sessionEndDate;
    /** tour = chờ lịch mới của tour; session = chờ chỗ trống của một đợt. */
    private String scope;
    private String status;
    private Instant createdAt;
}
