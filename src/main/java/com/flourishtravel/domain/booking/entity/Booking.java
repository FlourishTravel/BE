package com.flourishtravel.domain.booking.entity;

import com.flourishtravel.common.entity.BaseEntity;
import com.flourishtravel.domain.tour.entity.TourSession;
import com.flourishtravel.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings", indexes = {@Index(columnList = "user_id"), @Index(columnList = "session_id"), @Index(columnList = "status")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private TourSession session;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "guest_count", nullable = false)
    private Integer guestCount;

    @Column(name = "special_requests", columnDefinition = "TEXT")
    private String specialRequests;

    /** Số điện thoại liên hệ cho chuyến đi (nếu trống dùng User.phone). */
    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    /** Điểm đón (tour xe bus, v.v.). */
    @Column(name = "pickup_address", columnDefinition = "TEXT")
    private String pickupAddress;

    /** Danh sách tên khách, cách nhau bởi dấu phẩy (giữ để tương thích; đồng bộ từ bookingGuests nếu có). */
    @Column(name = "guest_names", columnDefinition = "TEXT")
    private String guestNames;

    /** Liên hệ khẩn cấp – tên người thân. */
    @Column(name = "emergency_contact_name", length = 255)
    private String emergencyContactName;

    /** Liên hệ khẩn cấp – SĐT người thân. */
    @Column(name = "emergency_contact_phone", length = 20)
    private String emergencyContactPhone;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "pending";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id")
    private Promotion promotion;

    @Column(name = "discount_amount", precision = 15, scale = 2)
    private BigDecimal discountAmount;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<com.flourishtravel.domain.payment.entity.Payment> payments = new ArrayList<>();

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<com.flourishtravel.domain.payment.entity.Refund> refunds = new ArrayList<>();

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<BookingGuest> bookingGuests = new ArrayList<>();
}
