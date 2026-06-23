package com.flourishtravel.domain.notification.push.entity;

import com.flourishtravel.common.entity.BaseEntity;
import com.flourishtravel.domain.notification.entity.Notification;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "push_notification_deliveries", indexes = {
        @Index(columnList = "status"),
        @Index(columnList = "next_attempt_at")
}, uniqueConstraints = {
        @UniqueConstraint(columnNames = {"notification_id", "push_device_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PushNotificationDelivery extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "push_device_id", nullable = false)
    private PushDevice pushDevice;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private Integer attemptCount = 0;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "provider_message_id", length = 128)
    private String providerMessageId;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "failure_reason_safe", length = 255)
    private String failureReasonSafe;
}
