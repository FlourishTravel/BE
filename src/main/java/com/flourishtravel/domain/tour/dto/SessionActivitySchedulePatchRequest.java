package com.flourishtravel.domain.tour.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
public class SessionActivitySchedulePatchRequest {

    private String title;
    private String description;
    private OffsetDateTime startAt;
    private OffsetDateTime endAt;
    private String locationName;
    private String locationAddress;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Boolean isGatheringEvent;
    private String gatheringEventType;
    private String scheduleStatus;
    private String operationalNote;
}
