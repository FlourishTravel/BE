package com.flourishtravel.domain.booking.entity;

import com.flourishtravel.common.entity.BaseEntity;
import com.flourishtravel.domain.tour.entity.TourActivity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Điểm danh / check-out của một người trên lịch khởi hành tại một điểm trong lịch trình tour
 * (hoạt động có {@link TourActivity#getLocationName()} / tiêu đề).
 */
@Entity
@Table(
        name = "session_participant_activity_attendance",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_participant_activity",
                columnNames = {"session_participant_id", "tour_activity_id"}
        ),
        indexes = {
                @Index(columnList = "session_participant_id"),
                @Index(columnList = "tour_activity_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionParticipantActivityAttendance extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_participant_id", nullable = false)
    private SessionParticipant sessionParticipant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tour_activity_id", nullable = false)
    private TourActivity tourActivity;

    @Column(name = "check_in_at")
    private Instant checkInAt;

    @Column(name = "check_out_at")
    private Instant checkOutAt;
}
