package com.flourishtravel.domain.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationViewDto {
    private UUID id;
    private String type;
    private String title;
    private String body;
    private String data;
    private Boolean isRead;
    private Instant readAt;
    private Instant createdAt;
}
