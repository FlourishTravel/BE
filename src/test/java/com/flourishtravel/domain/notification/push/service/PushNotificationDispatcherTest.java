package com.flourishtravel.domain.notification.push.service;

import com.flourishtravel.domain.flora.FloraReminderTypes;
import com.flourishtravel.domain.flora.service.FloraPrivacyService;
import com.flourishtravel.domain.notification.entity.Notification;
import com.flourishtravel.domain.notification.push.PushDeliveryStatus;
import com.flourishtravel.domain.notification.push.config.FcmPushProperties;
import com.flourishtravel.domain.notification.push.entity.PushDevice;
import com.flourishtravel.domain.notification.push.entity.PushNotificationDelivery;
import com.flourishtravel.domain.notification.push.fcm.FcmPushSender;
import com.flourishtravel.domain.notification.push.repository.PushDeviceRepository;
import com.flourishtravel.domain.notification.push.repository.PushNotificationDeliveryRepository;
import com.flourishtravel.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PushNotificationDispatcherTest {

    @Mock PushNotificationDeliveryRepository deliveryRepository;
    @Mock PushDeviceRepository deviceRepository;
    @Mock FloraPrivacyService privacyService;
    @Mock FcmPushSender fcmPushSender;
    @InjectMocks PushNotificationDispatcher dispatcher;

    private FcmPushProperties properties;

    @BeforeEach
    void setUp() {
        properties = new FcmPushProperties();
        properties.setEnabled(true);
        properties.setMaxAttempts(3);
        dispatcher = new PushNotificationDispatcher(
                deliveryRepository, deviceRepository, privacyService, fcmPushSender, properties);
    }

    @Test
    void dispatch_marksSentOnSuccess() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().build();
        user.setId(userId);
        Notification notification = Notification.builder()
                .user(user)
                .type(FloraReminderTypes.SCHEDULE_CHANGED)
                .build();
        notification.setId(UUID.randomUUID());
        PushDevice device = PushDevice.builder()
                .user(user)
                .tokenCiphertext("enc-token")
                .active(true)
                .notificationPermissionGranted(true)
                .build();
        device.setId(UUID.randomUUID());
        PushNotificationDelivery delivery = PushNotificationDelivery.builder()
                .notification(notification)
                .pushDevice(device)
                .status(PushDeliveryStatus.PENDING)
                .attemptCount(0)
                .build();
        delivery.setId(UUID.randomUUID());

        when(deliveryRepository.findPendingReady(any())).thenReturn(List.of(delivery));
        when(privacyService.hasNotificationConsent(userId)).thenReturn(true);
        when(fcmPushSender.send(anyString(), anyString(), anyString(), anyMap()))
                .thenReturn(new FcmPushSender.SendResult(FcmPushSender.SendOutcome.SENT, "msg-1", null));

        dispatcher.dispatchPending();

        assertEquals(PushDeliveryStatus.SENT, delivery.getStatus());
        verify(deliveryRepository).save(delivery);
    }

    @Test
    void dispatch_deactivatesInvalidToken() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().build();
        user.setId(userId);
        Notification notification = Notification.builder().user(user).type(FloraReminderTypes.POST_TOUR_FEEDBACK).build();
        notification.setId(UUID.randomUUID());
        PushDevice device = PushDevice.builder()
                .user(user)
                .tokenCiphertext("enc-token")
                .active(true)
                .notificationPermissionGranted(true)
                .build();
        PushNotificationDelivery delivery = PushNotificationDelivery.builder()
                .notification(notification)
                .pushDevice(device)
                .status(PushDeliveryStatus.PENDING)
                .attemptCount(0)
                .build();

        when(deliveryRepository.findPendingReady(any())).thenReturn(List.of(delivery));
        when(privacyService.hasNotificationConsent(userId)).thenReturn(true);
        when(fcmPushSender.send(anyString(), anyString(), anyString(), anyMap()))
                .thenReturn(new FcmPushSender.SendResult(FcmPushSender.SendOutcome.INVALID_TOKEN, null, "invalid_token"));

        dispatcher.dispatchPending();

        assertEquals(PushDeliveryStatus.INVALID_TOKEN, delivery.getStatus());
        assertFalse(device.getActive());
        verify(deviceRepository).save(device);
    }

    @Test
    void dispatch_retriesTemporaryFailure() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().build();
        user.setId(userId);
        Notification notification = Notification.builder().user(user).type(FloraReminderTypes.TOUR_REMINDER_5_MINUTES).build();
        notification.setId(UUID.randomUUID());
        PushDevice device = PushDevice.builder()
                .user(user)
                .tokenCiphertext("enc-token")
                .active(true)
                .notificationPermissionGranted(true)
                .build();
        PushNotificationDelivery delivery = PushNotificationDelivery.builder()
                .notification(notification)
                .pushDevice(device)
                .status(PushDeliveryStatus.PENDING)
                .attemptCount(0)
                .build();

        when(deliveryRepository.findPendingReady(any())).thenReturn(List.of(delivery));
        when(privacyService.hasNotificationConsent(userId)).thenReturn(true);
        when(fcmPushSender.send(anyString(), anyString(), anyString(), anyMap()))
                .thenReturn(new FcmPushSender.SendResult(FcmPushSender.SendOutcome.TEMPORARY_FAILURE, null, "temporary_failure"));

        dispatcher.dispatchPending();

        assertEquals(PushDeliveryStatus.PENDING, delivery.getStatus());
        assertEquals(1, delivery.getAttemptCount());
        assertNotNull(delivery.getNextAttemptAt());
    }
}
