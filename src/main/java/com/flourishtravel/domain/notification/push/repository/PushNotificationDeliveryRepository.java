package com.flourishtravel.domain.notification.push.repository;

import com.flourishtravel.domain.notification.push.entity.PushNotificationDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PushNotificationDeliveryRepository extends JpaRepository<PushNotificationDelivery, UUID> {

    boolean existsByNotification_IdAndPushDevice_Id(UUID notificationId, UUID pushDeviceId);

    @Query("""
            SELECT d FROM PushNotificationDelivery d
            JOIN FETCH d.notification n
            JOIN FETCH d.pushDevice pd
            JOIN FETCH pd.user
            WHERE d.status = 'PENDING'
              AND (d.nextAttemptAt IS NULL OR d.nextAttemptAt <= :now)
            ORDER BY d.createdAt ASC
            """)
    List<PushNotificationDelivery> findPendingReady(@Param("now") Instant now);
}
