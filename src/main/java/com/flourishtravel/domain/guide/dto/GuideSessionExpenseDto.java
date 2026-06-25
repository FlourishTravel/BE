package com.flourishtravel.domain.guide.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class GuideSessionExpenseDto {
    private UUID id;
    private UUID sessionId;
    private String tourTitle;
    private String tourCode;
    private String category;
    private String description;
    private Long amount;
    private String status;
    private LocalDate expenseDate;
    private String adminNote;
    private Instant createdAt;
}
