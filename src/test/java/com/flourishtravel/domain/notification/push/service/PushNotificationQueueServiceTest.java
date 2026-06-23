package com.flourishtravel.domain.notification.push.service;

import com.flourishtravel.domain.flora.FloraReminderTypes;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PushNotificationQueueServiceTest {

    @Mock NotificationRepository notificationRepository;
    @Mock PushDeviceRepository deviceRepository;
    @Mock PushNotificationDeliveryRepository deliveryRepository;
    @Mock FloraPrivacyService privacyService;
    @InjectMocks PushNotificationQueueService queueService;

    private FcmPushProperties properties;
    private UUID userId;
    private UUID notificationId;

    @BeforeEach
    void setUp() {
        properties = new FcmPushProperties();
        properties.setEnabled(true);
        queueService = new PushNotificationQueueService(
                notificationRepository, deviceRepository, deliveryRepository, privacyService, properties);
        userId = UUID.randomUUID();
        notificationId = UUID.randomUUID();
    }

    @Test
    void queue_skipsWhenConsentOff() {
        User user = User.builder().build();
        user.setId(userId);
        Notification n = Notification.builder().user(user).type(FloraReminderTypes.SCHEDULE_CHANGED).build();
        n.setId(notificationId);
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(n));
        when(privacyService.hasNotificationConsent(userId)).thenReturn(false);

        queueService.queueDeliveries(notificationId);
        verify(deliveryRepository, never()).save(any());
    }

    @Test
    void queue_createsDeliveryPerDevice() {
        User user = User.builder().build();
        user.setId(userId);
        Notification n = Notification.builder().user(user).type(FloraReminderTypes.SCHEDULE_CHANGED).build();
        n.setId(notificationId);
        PushDevice device = PushDevice.builder().user(user).notificationPermissionGranted(true).active(true).build();
        device.setId(UUID.randomUUID());

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(n));
        when(privacyService.hasNotificationConsent(userId)).thenReturn(true);
        when(deviceRepository.findByUserAndActiveTrue(user)).thenReturn(List.of(device));
        when(deliveryRepository.existsByNotification_IdAndPushDevice_Id(notificationId, device.getId())).thenReturn(false);

        queueService.queueDeliveries(notificationId);

        verify(deliveryRepository).save(argThat(d ->
                PushDeliveryStatus.PENDING.equals(d.getStatus())
                        && d.getNotification().getId().equals(notificationId)));
    }

    @Test
    void queue_skipsDuplicateDelivery() {
        User user = User.builder().build();
        user.setId(userId);
        Notification n = Notification.builder().user(user).type(FloraReminderTypes.POST_TOUR_FEEDBACK).build();
        n.setId(notificationId);
        PushDevice device = PushDevice.builder().user(user).notificationPermissionGranted(true).active(true).build();
        device.setId(UUID.randomUUID());

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(n));
        when(privacyService.hasNotificationConsent(userId)).thenReturn(true);
        when(deviceRepository.findByUserAndActiveTrue(user)).thenReturn(List.of(device));
        when(deliveryRepository.existsByNotification_IdAndPushDevice_Id(notificationId, device.getId())).thenReturn(true);

        queueService.queueDeliveries(notificationId);
        verify(deliveryRepository, never()).save(any());
    }

    @Test
    void queue_noOpWhenFcmDisabled() {
        properties.setEnabled(false);
        queueService.queueDeliveries(notificationId);
        verify(notificationRepository, never()).findById(any());
    }
}
