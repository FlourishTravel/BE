package com.flourishtravel.domain.notification.push.fcm;

import java.util.Map;

public interface FcmPushSender {

    enum SendOutcome {
        SENT,
        INVALID_TOKEN,
        TEMPORARY_FAILURE
    }

    record SendResult(SendOutcome outcome, String providerMessageId, String safeFailureReason) {}

    boolean isEnabled();

    SendResult send(String deviceToken, String title, String body, Map<String, String> data);
}
