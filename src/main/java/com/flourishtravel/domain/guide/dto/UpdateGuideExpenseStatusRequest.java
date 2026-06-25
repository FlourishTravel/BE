package com.flourishtravel.domain.guide.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateGuideExpenseStatusRequest {

    @NotBlank
    private String status;

    private String adminNote;
}
