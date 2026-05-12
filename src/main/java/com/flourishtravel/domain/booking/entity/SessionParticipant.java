package com.flourishtravel.domain.booking.entity;

import com.flourishtravel.common.entity.BaseEntity;
import com.flourishtravel.domain.tour.entity.TourSession;
import com.flourishtravel.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Một người tham gia cụ thể trên một lịch khởi hành (session).
 * Đồng bộ từ booking đã thanh toán: dòng 0 = người đặt (user), các dòng tiếp = {@link BookingGuest}.
 * HDV điểm danh / trả khách theo từng dòng (check-in / check-out).
 */
@Entity
@Table(
        name = "session_participants",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_session_booking_roster_key",
                        columnNames = {"session_id", "booking_id", "roster_key"}
                ),
                @UniqueConstraint(
                        name = "uk_session_booking_line",
                        columnNames = {"session_id", "booking_id", "line_index"}
                )
        },
        indexes = {
                @Index(columnList = "session_id"),
                @Index(columnList = "booking_id"),
                @Index(columnList = "user_id"),
                @Index(columnList = "booking_guest_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionParticipant extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private TourSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    /**
     * Null = người đặt (lead). Non-null = một dòng trong booking_guests.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_guest_id")
    private BookingGuest bookingGuest;

    /** Người đặt — luôn có với line_index = 0 khi đã đồng bộ. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * Khóa cố định trong đơn: "LEAD" hoặc UUID của booking_guest (String).
     * Giúp upsert khi KH sửa danh sách kèm mà không làm mất check-in.
     */
    @Column(name = "roster_key", nullable = false, length = 40)
    private String rosterKey;

    /** Thứ tự hiển thị: 0 = lead, 1..n = kèm (theo sort_order). */
    @Column(name = "line_index", nullable = false)
    private Integer lineIndex;

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    /** Ảnh chụp SĐT lúc đồng bộ (hiển thị HDV; có thể khác user.phone nếu đổi sau). */
    @Column(name = "phone_snapshot", length = 30)
    private String phoneSnapshot;

    /** LEAD | COMPANION */
    @Column(name = "participant_role", nullable = false, length = 20)
    private String participantRole;

    @Column(name = "check_in_at")
    private Instant checkInAt;

    @Column(name = "check_out_at")
    private Instant checkOutAt;
}
