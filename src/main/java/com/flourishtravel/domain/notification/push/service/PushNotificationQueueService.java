package com.flourishtravel.domain.notification.push.service;

import com.flourishtravel.domain.flora.service.FloraPrivacyService;
import com.flourishtravel.domain.notification.entity.Notification;
import com.flourishtravel.domain.notification.push.PushDeliveryStatus;
import com.flourishtravel.domain.notification.push.config.FcmPushProperties;
import com.flourishtravel.domain.notification.push.entity.PushDevice;
import com.flourishtravel.domain.notification.push.entity.PushNotificationDelivery;
import com.flourishtravel.domain.notification.push.repository.PushDeviceRepository;
import com.flourishtravel.domain.notification.push.repository.PushNotificationDeliveryRepository;
import com.flourishtravel.domain.notification.repository.NotificationRepository;
import com.flourishtravel.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationQueueService {

    private final NotificationRepository notificationRepository;
    private final PushDeviceRepository deviceRepository;
    private final PushNotificationDeliveryRepository deliveryRepository;
    private final FloraPrivacyService privacyService;
    private final FcmPushProperties properties;

    public void scheduleQueueAfterCommit(UUID notificationId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            queueDeliveries(notificationId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                queueDeliveries(notificationId);
            }
        });
    }

    @Transactional
    public void queueDeliveries(UUID notificationId) {
        if (!properties.isEnabled()) {
            return;
        }
        Notification notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification == null) return;

        User user = notification.getUser();
        if (user == null) return;

        if (!privacyService.hasNotificationConsent(user.getId())) {
            log.debug("event=push_skipped_consent notification_id={}", notificationId);
            return;
        }

        String type = notification.getType() != null ? notification.getType().trim().toUpperCase(Locale.ROOT) : "";
        if (!isAllowedType(type)) {
            return;
        }

        List<PushDevice> devices = deviceRepository.findByUserAndActiveTrue(user).stream()
                .filter(d -> Boolean.TRUE.equals(d.getNotificationPermissionGranted()))
                .toList();
        if (devices.isEmpty()) {
            return;
        }

        for (PushDevice device : devices) {
            if (deliveryRepository.existsByNotification_IdAndPushDevice_Id(notification.getId(), device.getId())) {
                continue;
            }
            PushNotificationDelivery delivery = PushNotificationDelivery.builder()
                    .notification(notification)
                    .pushDevice(device)
                    .status(PushDeliveryStatus.PENDING)
                    .attemptCount(0)
                    .nextAttemptAt(Instant.now())
                    .build();
            deliveryRepository.save(delivery);
            log.debug("event=push_queued notification_id={} device_id={}", notificationId, device.getId());
        }
    }

    private boolean isAllowedType(String type) {
        return properties.getAllowedTypes().stream()
                .anyMatch(t -> t.equalsIgnoreCase(type));
    }
}
