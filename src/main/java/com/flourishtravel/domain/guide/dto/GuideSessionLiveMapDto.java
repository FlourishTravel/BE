package com.flourishtravel.domain.guide.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Vị trí đoàn cho HDV — chỉ có tọa độ khi session đang diễn ra. */
@Data
@Builder
public class GuideSessionLiveMapDto {

    private boolean live;
    private String message;
    private LocalDate sessionStartDate;
    private LocalDate sessionEndDate;
    private Instant generatedAt;
    private int freshCount;
    private int staleCount;
    private List<Marker> markers;

    @Data
    @Builder
    public static class Marker {
        private UUID bookingId;
        private String bookingCode;
        private String displayName;
        private Double latitude;
        private Double longitude;
        private Instant capturedAt;
        private boolean stale;
    }
}
