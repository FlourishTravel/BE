package com.flourishtravel.domain.notification.push.entity;

import com.flourishtravel.common.converter.PiiEncryptionConverter;
import com.flourishtravel.common.entity.BaseEntity;
import com.flourishtravel.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "push_devices", indexes = {
        @Index(columnList = "user_id"),
        @Index(columnList = "token_hash", unique = true),
        @Index(columnList = "active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PushDevice extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Convert(converter = PiiEncryptionConverter.class)
    @Column(name = "token_ciphertext", nullable = false, columnDefinition = "TEXT")
    private String tokenCiphertext;

    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String platform = "ANDROID";

    @Column(name = "app_version", length = 32)
    private String appVersion;

    @Column(name = "notification_permission_granted")
    @Builder.Default
    private Boolean notificationPermissionGranted = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;
}
