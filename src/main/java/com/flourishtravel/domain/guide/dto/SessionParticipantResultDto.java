package com.flourishtravel.domain.guide.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class SessionParticipantResultDto {
    private UUID participantId;
    private UUID sessionId;
    private UUID bookingId;
    private String rosterKey;
    private int lineIndex;
    private String displayName;
    private String phoneSnapshot;
    private String participantRole;
    private Instant checkInAt;
    private Instant checkOutAt;
}
