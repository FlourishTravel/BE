package com.flourishtravel.domain.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Thống kê tổng quan cho trang admin Quản Lý Đặt Chỗ.
 * Bao gồm các chỉ số dùng cho StatCards.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingStatsDto {

    /** Tổng số booking trong khoảng (mặc định tháng hiện tại). */
    private long totalBookings;

    /** Doanh thu (sum totalAmount của booking có ít nhất 1 payment = paid) trong khoảng. */
    private BigDecimal monthlyRevenue;

    /** Tổng số tiền cọc chưa thanh toán đủ (balance > 0) cho các booking chưa hoàn thành. */
    private BigDecimal pendingDeposit;

    /** Số yêu cầu hoàn tiền đang chờ xử lý. */
    private long pendingRefundRequests;

    /** Số booking theo từng trạng thái (key chuẩn hoá: pending, paid, confirmed, completed, cancelled). */
    private StatusBreakdown breakdown;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusBreakdown {
        private long pending;
        private long paid;
        private long confirmed;
        private long completed;
        private long cancelled;
        private long refundPending;
    }
}
