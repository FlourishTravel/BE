package com.flourishtravel.domain.booking.dto;

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
 * Payload chi tiết 1 booking cho modal admin (xem & xử lý refund / đổi trạng thái).
 * Bao gồm thông tin tài chính, danh sách khách, payments, refunds, promotion áp dụng.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminBookingDetailDto {

    private UUID id;
    private String bookingCode;
    private String status;
    private boolean hasRefundPending;

    private AdminBookingSummaryDto.CustomerRef customer;
    private AdminBookingSummaryDto.TourRef tour;
    private AdminBookingSummaryDto.SessionRef session;

    private Integer guestCount;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal paidAmount;
    private BigDecimal balanceAmount;
    private BigDecimal refundedAmount;
    private String paymentClass;

    private String contactPhone;
    private String pickupAddress;
    private String specialRequests;
    private String emergencyContactName;
    private String emergencyContactPhone;

    private PromotionRef promotion;

    private List<GuestRef> guests;
    private List<PaymentRef> payments;
    private List<RefundRef> refunds;

    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GuestRef {
        private UUID id;
        private String fullName;
        private String maskedIdNumber;   // ***1234
        private LocalDate dateOfBirth;
        private Integer ageAtDeparture;  // tính theo session.startDate
        private Integer sortOrder;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentRef {
        private UUID id;
        private String orderId;
        private String provider;
        private BigDecimal amount;
        private String status;
        private String providerTransId;
        private Instant createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefundRef {
        private UUID id;
        private BigDecimal amount;
        private String reason;
        private String status;             // pending | approved | rejected | completed
        private String processedByName;
        private Instant processedAt;
        private String providerRefundId;
        private Instant createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PromotionRef {
        private UUID id;
        private String code;
        private String name;
        private String discountType;
        private BigDecimal discountValue;
    }
}
