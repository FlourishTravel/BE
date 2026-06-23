package com.flourishtravel.domain.notification.push.service;

import com.flourishtravel.domain.flora.service.FloraPrivacyService;
import com.flourishtravel.domain.notification.entity.Notification;
import com.flourishtravel.domain.notification.push.PushContentSanitizer;
import com.flourishtravel.domain.notification.push.PushDeliveryStatus;
import com.flourishtravel.domain.notification.push.config.FcmPushProperties;
import com.flourishtravel.domain.notification.push.entity.PushDevice;
import com.flourishtravel.domain.notification.push.entity.PushNotificationDelivery;
import com.flourishtravel.domain.notification.push.fcm.FcmPushSender;
import com.flourishtravel.domain.notification.push.repository.PushDeviceRepository;
import com.flourishtravel.domain.notification.push.repository.PushNotificationDeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationDispatcher {

    private final PushNotificationDeliveryRepository deliveryRepository;
    private final PushDeviceRepository deviceRepository;
    private final FloraPrivacyService privacyService;
    private final FcmPushSender fcmPushSender;
    private final FcmPushProperties properties;

    @Scheduled(fixedRateString = "${app.push.fcm.dispatcher-poll-ms:60000}")
    @Transactional
    public void dispatchPending() {
        if (!properties.isEnabled()) {
            return;
        }
        Instant now = Instant.now();
        List<PushNotificationDelivery> pending = deliveryRepository.findPendingReady(now);
        for (PushNotificationDelivery delivery : pending) {
            processOne(delivery, now);
        }
    }

    private void processOne(PushNotificationDelivery delivery, Instant now) {
        Notification notification = delivery.getNotification();
        PushDevice device = delivery.getPushDevice();
        if (notification == null || device == null || !Boolean.TRUE.equals(device.getActive())) {
            markSkipped(delivery, "device_inactive");
            return;
        }
        UUID userId = device.getUser().getId();
        if (!privacyService.hasNotificationConsent(userId)) {
            markSkipped(delivery, "consent_off");
            log.debug("event=push_skipped_consent delivery_id={}", delivery.getId());
            return;
        }
        if (!Boolean.TRUE.equals(device.getNotificationPermissionGranted())) {
            markSkipped(delivery, "permission_denied");
            return;
        }

        String token = device.getTokenCiphertext();
        if (token == null || token.isBlank()) {
            markSkipped(delivery, "missing_token");
            return;
        }

        PushContentSanitizer.SafePushCopy copy = PushContentSanitizer.forType(notification.getType());
        Map<String, String> data = new HashMap<>();
        data.put("notificationId", notification.getId().toString());
        data.put("target", "FLORA_NOTIFICATION");

        FcmPushSender.SendResult result = fcmPushSender.send(token, copy.title(), copy.body(), data);
        delivery.setAttemptCount(delivery.getAttemptCount() + 1);

        switch (result.outcome()) {
            case SENT -> {
                delivery.setStatus(PushDeliveryStatus.SENT);
                delivery.setSentAt(now);
                delivery.setProviderMessageId(result.providerMessageId());
                delivery.setFailureReasonSafe(null);
                log.info("event=push_sent delivery_id={}", delivery.getId());
            }
            case INVALID_TOKEN -> {
                delivery.setStatus(PushDeliveryStatus.INVALID_TOKEN);
                delivery.setFailureReasonSafe(result.safeFailureReason());
                device.setActive(false);
                deviceRepository.save(device);
                log.info("event=push_invalid_token device_id={}", device.getId());
            }
            case TEMPORARY_FAILURE -> handleRetry(delivery, result.safeFailureReason(), now);
        }
        deliveryRepository.save(delivery);
    }

    private void handleRetry(PushNotificationDelivery delivery, String reason, Instant now) {
        int max = Math.max(1, properties.getMaxAttempts());
        if (delivery.getAttemptCount() >= max) {
            delivery.setStatus(PushDeliveryStatus.FAILED);
            delivery.setFailureReasonSafe(reason);
            log.info("event=push_failed delivery_id={}", delivery.getId());
            return;
        }
        delivery.setStatus(PushDeliveryStatus.PENDING);
        delivery.setFailureReasonSafe(reason);
        delivery.setNextAttemptAt(now.plusSeconds(60L * delivery.getAttemptCount()));
        log.debug("event=push_retry delivery_id={}", delivery.getId());
    }

    private void markSkipped(PushNotificationDelivery delivery, String reason) {
        delivery.setStatus(PushDeliveryStatus.SKIPPED);
        delivery.setFailureReasonSafe(reason);
        deliveryRepository.save(delivery);
    }
}
