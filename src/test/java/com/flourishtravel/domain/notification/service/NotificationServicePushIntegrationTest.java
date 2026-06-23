package com.flourishtravel.domain.notification.service;

import com.flourishtravel.domain.notification.entity.Notification;
import com.flourishtravel.domain.notification.push.service.PushNotificationQueueService;
import com.flourishtravel.domain.notification.repository.NotificationRepository;
import com.flourishtravel.domain.user.entity.User;
import com.flourishtravel.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServicePushIntegrationTest {

    @Mock NotificationRepository notificationRepository;
    @Mock UserRepository userRepository;
    @Mock PushNotificationQueueService pushNotificationQueueService;
    @InjectMocks NotificationService notificationService;

    @Test
    void createFloraNotification_alwaysCreatesInAppAndSchedulesPush() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().email("u@u.com").build();
        user.setId(userId);
        Notification saved = Notification.builder().user(user).type("SCHEDULE_CHANGED").build();
        saved.setId(UUID.randomUUID());

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(notificationRepository.save(any())).thenReturn(saved);

        notificationService.createFloraNotification(userId, "SCHEDULE_CHANGED", "t", "b", null);

        verify(notificationRepository).save(any(Notification.class));
        verify(pushNotificationQueueService).scheduleQueueAfterCommit(saved.getId());
    }
}
