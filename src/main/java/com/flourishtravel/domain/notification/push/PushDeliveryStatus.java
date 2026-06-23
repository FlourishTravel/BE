package com.flourishtravel.domain.notification.push;

public final class PushDeliveryStatus {

    private PushDeliveryStatus() {}

    public static final String PENDING = "PENDING";
    public static final String SENT = "SENT";
    public static final String FAILED = "FAILED";
    public static final String INVALID_TOKEN = "INVALID_TOKEN";
    public static final String SKIPPED = "SKIPPED";
}
