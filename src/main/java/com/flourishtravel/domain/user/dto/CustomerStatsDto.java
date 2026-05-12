package com.flourishtravel.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Stats tổng quan cho trang admin Quản Lý Khách Hàng (4 StatCards).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerStatsDto {

    /** Tổng số KH (role=TRAVELER, không tính ADMIN/TOUR_GUIDE). */
    private long totalCustomers;

    /** Số KH mới gia nhập tháng này. */
    private long newCustomersThisMonth;

    /** Số KH VIP (totalSpent ≥ 100M). */
    private long vipCustomers;

    /** Tỷ lệ quay lại (%): KH có >=2 booking / tổng KH có >=1 booking. */
    private double returnRatePercent;

    /** Chi tiêu TB / KH (chỉ tính KH đã chi tiêu). */
    private BigDecimal averageSpendPerCustomer;

    /** Phân bố theo hạng (VIP / Gold / Silver / Standard). */
    private TierBreakdown breakdown;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TierBreakdown {
        private long vip;
        private long gold;
        private long silver;
        private long standard;
    }
}
