package com.flourishtravel.domain.booking.entity;

import com.flourishtravel.common.entity.BaseEntity;
import com.flourishtravel.domain.tour.entity.TourSession;
import com.flourishtravel.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "session_checkins", indexes = {
    @Index(columnList = "session_id"),
    @Index(columnList = "user_id"),
    @Index(columnList = "session_id, user_id, check_in_type", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionCheckin extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private TourSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "check_in_type", nullable = false, length = 50)
    private String checkInType;
}
