package com.flourishtravel.domain.tour.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/** Kết quả kiểm tra còn chỗ tour (cho chatbot / frontend). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AvailabilityCheckDto {

    private Integer remainingSlots;
    private LocalDate nextStartDate;
    private String tourTitle;
    private UUID tourId;
    private UUID sessionId;
}
