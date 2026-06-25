package com.flourishtravel.domain.user.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
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
}
