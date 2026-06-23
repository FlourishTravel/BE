package com.flourishtravel.domain.notification.push.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PushDeviceUnregisterRequest {

    @NotBlank
    @Size(min = 20, max = 4096)
    private String token;
}
