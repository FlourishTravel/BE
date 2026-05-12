package com.flourishtravel.domain.guide.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ParticipantActivityAttendanceResultDto {
    private UUID id;
    private UUID sessionParticipantId;
    private UUID tourActivityId;
    private Instant checkInAt;
    private Instant checkOutAt;
}
