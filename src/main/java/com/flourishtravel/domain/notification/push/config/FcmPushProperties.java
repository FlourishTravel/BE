package com.flourishtravel.domain.notification.push.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "app.push.fcm")
@Getter
@Setter
public class FcmPushProperties {

    private boolean enabled = false;
    private String credentialsPath = "";
    private int maxDevicesPerUser = 5;
    private int maxAttempts = 3;
    private long dispatcherPollMs = 60_000L;
    private List<String> allowedTypes = List.of(
            "TOUR_REMINDER_30_MINUTES",
            "TOUR_REMINDER_15_MINUTES",
            "TOUR_REMINDER_5_MINUTES",
            "RETURN_TO_BUS_ALERT",
            "SCHEDULE_CHANGED",
            "POST_TOUR_FEEDBACK"
    );
}
