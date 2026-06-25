package com.flourishtravel.domain.guide.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateGuideSessionExpenseRequest {

    @NotBlank
    private String category;

    @NotBlank
    private String description;

    @NotNull
    @Min(1)
    private Long amount;

    private LocalDate expenseDate;
}
