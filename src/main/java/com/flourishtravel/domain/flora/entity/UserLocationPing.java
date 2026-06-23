package com.flourishtravel.domain.flora.entity;

import com.flourishtravel.common.entity.BaseEntity;
import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "user_location_pings", indexes = {
        @Index(columnList = "booking_id"),
        @Index(columnList = "user_id"),
        @Index(columnList = "captured_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLocationPing extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "accuracy_meters")
    private Double accuracyMeters;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;
}
