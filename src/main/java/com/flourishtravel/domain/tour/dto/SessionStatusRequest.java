package com.flourishtravel.domain.tour.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload đổi trạng thái session (scheduled | cancelled | completed).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionStatusRequest {

    @NotBlank
    @Pattern(regexp = "^(scheduled|cancelled|completed)$",
             message = "status phải là scheduled, cancelled hoặc completed")
    private String status;

    private String note;
}
