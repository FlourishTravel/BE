package com.flourishtravel.domain.notification.push.fcm;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.push.fcm.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpFcmPushSender implements FcmPushSender {

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public SendResult send(String deviceToken, String title, String body, Map<String, String> data) {
        return new SendResult(SendOutcome.TEMPORARY_FAILURE, null, "fcm_disabled");
    }
}
