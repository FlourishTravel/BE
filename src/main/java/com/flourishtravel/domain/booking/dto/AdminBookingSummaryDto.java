package com.flourishtravel.domain.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Payload tóm tắt 1 booking cho bảng admin Quản Lý Đặt Chỗ.
 *
 * Bao gồm:
 *  - Thông tin booking & khách hàng (đã ẩn PII)
 *  - Tour + session
 *  - Tổng hợp thanh toán (paidAmount, balance, paymentClass)
 *  - Cờ has refund pending để hiển thị badge yêu cầu hoàn tiền
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminBookingSummaryDto {

    private UUID id;
    private String bookingCode;          // "FT-XXXXXXXX" (8 ký tự đầu UUID)
    private String status;               // pending | paid | confirmed | completed | cancelled
    private boolean hasRefundPending;    // true nếu tồn tại refund.status = 'pending'

    private CustomerRef customer;
    private TourRef tour;
    private SessionRef session;

    private Integer guestCount;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal paidAmount;       // tổng payments status=paid
    private BigDecimal balanceAmount;    // totalAmount - paidAmount (>=0)

    /** Suy luận: paid | partial | unpaid | refunded | refund_pending. */
    private String paymentClass;

    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerRef {
        private UUID id;
        private String fullName;
        private String email;
        private String phone;
        private String avatarUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TourRef {
        private UUID id;
        private String title;
        private String slug;
        private String tourCode;
        private String thumbnailUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionRef {
        private UUID id;
        private LocalDate startDate;
        private LocalDate endDate;
        private String status;
        private Integer maxParticipants;
        private Integer currentParticipants;
    }
}
