package com.flourishtravel.domain.notification.push.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PushDeviceRegisterResponse {
    boolean registered;
    boolean pushEnabled;
    boolean devicePermissionGranted;
}
