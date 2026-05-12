package com.flourishtravel.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Chi tiết 1 khách hàng cho modal admin (bao gồm hồ sơ, thống kê, bookings, favorites, activity).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCustomerDetailDto {

    private UUID id;
    private String email;
    private String fullName;
    private String phone;
    private String avatarUrl;
    private String role;
    private boolean active;
    private boolean marketingOptIn;

    private LocalDate dateOfBirth;
    private Integer age;
    private String gender;
    private String address;
    private String nationality;
    private String adminNote;
    private Instant lastLoginAt;

    private long bookingCount;
    private long completedBookingCount;
    private long cancelledBookingCount;
    private BigDecimal totalSpent;
    private BigDecimal averageOrderValue;
    private BigDecimal pendingRefundAmount;
    private String tier;
    private Instant joinedAt;
    private Instant lastActiveAt;
    private Integer membershipMonths;

    private List<BookingRef> recentBookings;
    private List<FavoriteRef> favoriteTours;
    private List<ActivityRef> activities;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookingRef {
        private UUID id;
        private String bookingCode;
        private String status;
        private String tourTitle;
        private LocalDate startDate;
        private Integer guestCount;
        private BigDecimal totalAmount;
        private BigDecimal paidAmount;
        private Instant createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FavoriteRef {
        private UUID tourId;
        private String tourTitle;
        private String thumbnailUrl;
        private Instant addedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityRef {
        /** booking_created | booking_paid | booking_cancelled | refund_requested | favorite_added | account_created. */
        private String type;
        private String text;
        private Instant at;
    }
}
