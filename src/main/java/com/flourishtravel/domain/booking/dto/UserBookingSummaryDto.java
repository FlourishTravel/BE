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
 * Danh sách "Chuyến đi của tôi" — không lộ entity User/Booking ra JSON (tránh passwordHash, quan hệ thừa).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBookingSummaryDto {

    private UUID bookingId;
    /** pending | paid | cancelled (theo booking) */
    private String bookingStatus;
    private Integer guestCount;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private Instant bookedAt;

    private UUID sessionId;
    private LocalDate sessionStartDate;
    private LocalDate sessionEndDate;
    private String sessionStatus;

    private UUID tourId;
    private String tourTitle;
    private String tourSlug;
    /** Ảnh đại diện (sort đầu tiên) */
    private String tourThumbnailUrl;
    private Integer tourDurationDays;
    private Integer tourDurationNights;
    private String categoryName;

    /** Email tài khoản đặt — tiện điền form hủy/chính sách (không bắt buộc hiển thị). */
    private String customerEmail;

    /** pending | paid | failed | refunded — từ bản ghi thanh toán mới nhất */
    private String paymentStatus;
    private String paymentOrderId;

    /** Có yêu cầu hoàn tiền đang chờ duyệt */
    private boolean refundPending;
}
