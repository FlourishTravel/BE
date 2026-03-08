package com.flourishtravel.domain.booking.entity;

import com.flourishtravel.common.entity.BaseEntity;
import com.flourishtravel.domain.tour.entity.Tour;
import com.flourishtravel.domain.tour.entity.TourSession;
import com.flourishtravel.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "session_waitlist", indexes = {
        @Index(columnList = "session_id"),
        @Index(columnList = "user_id"),
        @Index(columnList = "tour_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionWaitlist extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private TourSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id")
    private Tour tour;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "waiting";
}
