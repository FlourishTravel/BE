package com.flourishtravel.domain.notification.service;

import com.flourishtravel.common.exception.ResourceNotFoundException;
import com.flourishtravel.domain.notification.entity.Notification;
import com.flourishtravel.domain.notification.repository.NotificationRepository;
import com.flourishtravel.domain.user.entity.User;
import com.flourishtravel.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    @Transactional(readOnly = true)
    public Page<Notification> getMyNotifications(UUID userId, Boolean unreadOnly, Integer limit) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        int size = limit != null && limit > 0 ? Math.min(limit, MAX_LIMIT) : DEFAULT_LIMIT;
        PageRequest page = PageRequest.of(0, size);
        if (Boolean.TRUE.equals(unreadOnly)) {
            return notificationRepository.findByUserAndIsReadFalseOrderByCreatedAtDesc(user, page);
        }
        return notificationRepository.findByUserOrderByCreatedAtDesc(user, page);
    }

    @Transactional
    public Notification markAsRead(UUID notificationId, UUID userId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));
        if (!n.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Notification", notificationId);
        }
        n.setIsRead(true);
        n.setReadAt(Instant.now());
        return notificationRepository.save(n);
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        notificationRepository.findByUserAndIsReadFalseOrderByCreatedAtDesc(user, PageRequest.of(0, 1000))
                .getContent()
                .forEach(n -> {
                    n.setIsRead(true);
                    n.setReadAt(Instant.now());
                    notificationRepository.save(n);
                });
    }
}
