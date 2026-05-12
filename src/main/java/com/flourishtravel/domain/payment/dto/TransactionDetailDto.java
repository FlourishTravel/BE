package com.flourishtravel.domain.payment.dto;

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
 * Chi tiết 1 giao dịch (payment hoặc refund) cho modal admin.
 *
 * Kèm thông tin booking + lịch sử payments/refunds liên quan.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDetailDto {

    private UUID id;
    private String kind;
    private String code;

    // Gateway / provider
    private String provider;
    private String partnerCode;
    private String orderId;
    private String requestId;
    private String providerTransId;
    private String signature;

    private BigDecimal amount;
    private BigDecimal feeAmount;
    private BigDecimal netAmount;
    private String currency;

    private String status;
    private String typeLabel;

    private Instant createdAt;
    private Instant paidAt;
    private Instant processedAt;
    private String processedByName;

    private String reason;
    private String failureReason;
    private String adminNote;

    private BookingContext booking;

    /** Tất cả payments cùng booking — phục vụ admin xem flow. */
    private List<RelatedPayment> relatedPayments;
    /** Tất cả refunds cùng booking. */
    private List<RelatedRefund> relatedRefunds;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookingContext {
        private UUID id;
        private String code;
        private String status;
        private BigDecimal totalAmount;
        private BigDecimal discountAmount;
        private Integer guestCount;
        private LocalDate departureDate;
        private String customerName;
        private String customerEmail;
        private String customerPhone;
        private String tourTitle;
        private String tourSlug;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelatedPayment {
        private UUID id;
        private String code;
        private String provider;
        private BigDecimal amount;
        private String status;
        private Instant createdAt;
        private Instant paidAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelatedRefund {
        private UUID id;
        private String code;
        private BigDecimal amount;
        private String status;
        private String reason;
        private Instant createdAt;
        private Instant processedAt;
    }
}
