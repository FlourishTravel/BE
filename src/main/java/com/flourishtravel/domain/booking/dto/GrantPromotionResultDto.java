package com.flourishtravel.domain.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrantPromotionResultDto {
    private int granted;
    private int skipped;
    private List<PromotionAssigneeDto> assignees;
}
