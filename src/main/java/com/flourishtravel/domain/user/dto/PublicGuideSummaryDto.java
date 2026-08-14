package com.flourishtravel.domain.user.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class PublicGuideSummaryDto {
    private UUID id;
    private String fullName;
    private String avatarUrl;
    private String jobTitle;
    private String department;
    private List<String> languages;
    private BigDecimal rating;
    private long toursCompleted;
    private List<AssignedTourRef> tours;

    @Data
    @Builder
    public static class AssignedTourRef {
        private UUID id;
        private String title;
        private Integer durationDays;
        private Integer durationNights;
        private BigDecimal basePrice;
        private String thumbnailUrl;
        private LocalDate nextStartDate;
    }
}
