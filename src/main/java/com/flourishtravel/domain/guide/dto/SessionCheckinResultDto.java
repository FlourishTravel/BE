package com.flourishtravel.domain.guide.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class SessionCheckinResultDto {
    private UUID id;
    private UUID sessionId;
    private UUID userId;
    private String checkInType;
}
