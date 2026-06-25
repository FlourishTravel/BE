package com.flourishtravel.domain.notification.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class AdminNotificationSummaryDto {
    private UUID id;
    private String title;
    private String body;
    private String type;
    private String recipientEmail;
    private Instant createdAt;
}
