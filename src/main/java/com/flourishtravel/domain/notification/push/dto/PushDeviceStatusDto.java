package com.flourishtravel.domain.notification.push.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PushDeviceStatusDto {
    boolean pushEnabled;
    boolean notificationConsent;
    boolean devicePermissionGranted;
    int activeDeviceCount;
}
