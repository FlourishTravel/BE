package com.flourishtravel.domain.notification.push.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PushDeviceRegisterRequest {

    @NotBlank
    @Size(min = 20, max = 4096)
    private String token;

    @NotBlank
    private String platform;

    private String appVersion;

    private Boolean notificationPermissionGranted;
}
