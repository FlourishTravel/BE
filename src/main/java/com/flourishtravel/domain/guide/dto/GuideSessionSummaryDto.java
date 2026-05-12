package com.flourishtravel.domain.guide.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class GuideSessionSummaryDto {
    private UUID sessionId;
    private UUID tourId;
    private String tourTitle;
    private String tourCode;
    private String thumbnailUrl;
    private String location;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private int currentParticipants;
    private int maxParticipants;
    private int checkedInParticipants;
}
