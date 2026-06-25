package com.flourishtravel.domain.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BroadcastNotificationRequest {
    @NotBlank
    private String title;
    @NotBlank
    private String body;
    private String type;
    private String targetRole;
}
