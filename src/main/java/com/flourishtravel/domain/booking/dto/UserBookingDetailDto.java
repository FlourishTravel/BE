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
 * Chi tiết một đơn đặt tour của khách — mở rộng {@link UserBookingSummaryDto}, gắn API GET /bookings/{id}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBookingDetailDto {

    private UUID bookingId;
    private String bookingStatus;
    private Integer guestCount;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private Instant bookedAt;
    private Instant updatedAt;

    private UUID sessionId;
    private LocalDate sessionStartDate;
    private LocalDate sessionEndDate;
    private String sessionStatus;
    private Integer sessionMaxParticipants;
    private Integer sessionCurrentParticipants;

    private UUID tourId;
    private String tourTitle;
    private String tourSlug;
    private String tourThumbnailUrl;
    private Integer tourDurationDays;
    private Integer tourDurationNights;
    private String categoryName;

    private String customerEmail;
    private String customerPhone;

    private String paymentStatus;
    private String paymentOrderId;
    private boolean refundPending;

    /** Mã khuyến mãi đã áp dụng (nếu có). */
    private String promotionCode;

    private String specialRequests;
    private String contactPhone;
    private String pickupAddress;
    private String guestNames;
    private String emergencyContactName;
    private String emergencyContactPhone;

    private String guideName;

    /** Khi đơn chờ thanh toán — link tiếp tục thanh toán (trang FE + query). */
    private String continuePaymentUrl;

    private List<UserBookingGuestLineDto> guests;
    private List<UserBookingPaymentLineDto> payments;
    private List<UserBookingRefundLineDto> refunds;
}
