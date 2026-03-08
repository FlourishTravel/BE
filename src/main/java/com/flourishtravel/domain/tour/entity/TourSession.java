package com.flourishtravel.domain.tour.entity;

import com.flourishtravel.domain.user.entity.User;
import com.flourishtravel.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tour_sessions", indexes = {@Index(columnList = "tour_id"), @Index(columnList = "tour_guide_id"), @Index(columnList = "start_date")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TourSession extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id", nullable = false)
    private Tour tour;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "max_participants", nullable = false)
    private Integer maxParticipants;

    @Column(name = "current_participants", nullable = false)
    @Builder.Default
    private Integer currentParticipants = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_guide_id")
    private User tourGuide;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "scheduled";
}
