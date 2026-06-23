package com.flourishtravel.domain.notification.push.fcm;

import com.flourishtravel.domain.notification.push.config.FcmPushProperties;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.push.fcm.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class FirebaseFcmPushSender implements FcmPushSender {

    private final FcmPushProperties properties;
    private volatile boolean initialized;

    @PostConstruct
    void init() {
        if (!properties.isEnabled()) {
            return;
        }
        String path = properties.getCredentialsPath();
        if (path == null || path.isBlank()) {
            log.warn("FCM enabled but app.push.fcm.credentials-path is empty — push sender inactive");
            return;
        }
        try (FileInputStream stream = new FileInputStream(path)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(stream))
                    .build();
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
            initialized = true;
            log.info("FCM push sender initialized");
        } catch (IOException e) {
            log.warn("FCM initialization failed — push sender inactive");
        }
    }

    @Override
    public boolean isEnabled() {
        return properties.isEnabled() && initialized;
    }

    @Override
    public SendResult send(String deviceToken, String title, String body, Map<String, String> data) {
        if (!isEnabled()) {
            return new SendResult(SendOutcome.TEMPORARY_FAILURE, null, "fcm_unavailable");
        }
        try {
            Message message = Message.builder()
                    .setToken(deviceToken)
                    .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                    .putAllData(data)
                    .build();
            String messageId = FirebaseMessaging.getInstance().send(message);
            return new SendResult(SendOutcome.SENT, messageId, null);
        } catch (FirebaseMessagingException ex) {
            MessagingErrorCode code = ex.getMessagingErrorCode();
            if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
                return new SendResult(SendOutcome.INVALID_TOKEN, null, "invalid_token");
            }
            return new SendResult(SendOutcome.TEMPORARY_FAILURE, null, "temporary_failure");
        } catch (Exception ex) {
            return new SendResult(SendOutcome.TEMPORARY_FAILURE, null, "temporary_failure");
        }
    }
}
