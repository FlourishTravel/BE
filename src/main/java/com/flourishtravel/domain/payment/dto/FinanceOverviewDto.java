package com.flourishtravel.domain.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Tổng quan tài chính cho dashboard admin Tài Chính.
 *
 * Chứa:
 *  - Stat cards (revenue, net, deposit, refund...)
 *  - Doanh thu theo tháng (cho mini-chart)
 *  - Phân bổ theo cổng thanh toán
 *  - Top tours theo doanh thu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinanceOverviewDto {

    // ---------- Stat cards ----------

    /** Tổng doanh thu (mọi thanh toán status=paid). */
    private BigDecimal totalRevenue;

    /** Doanh thu tháng hiện tại. */
    private BigDecimal monthlyRevenue;

    /** Doanh thu tháng trước (so sánh). */
    private BigDecimal previousMonthRevenue;

    /** % thay đổi so với tháng trước (có thể âm). */
    private double monthlyChangePercent;

    /** Tổng tiền đã hoàn (refund.status=processed). */
    private BigDecimal refundedAmount;

    /** Tổng tiền yêu cầu hoàn còn pending (refund.status=pending). */
    private BigDecimal pendingRefundAmount;

    /** Doanh thu ròng = totalRevenue - refundedAmount - feeAmount. */
    private BigDecimal netRevenue;

    /** Tổng phí cổng thanh toán đã trả. */
    private BigDecimal totalFees;

    /** Đang chờ thu (booking pending có balance dương). */
    private BigDecimal pendingCollection;

    /** Đếm giao dịch (payments) tháng này. */
    private long transactionsThisMonth;

    /** Đếm giao dịch thành công / tổng — tính tỷ lệ thành công. */
    private double successRatePercent;

    /** Giá trị giao dịch trung bình (paid). */
    private BigDecimal averageTransactionValue;

    // ---------- Charts ----------

    private List<MonthlyRevenuePoint> revenueByMonth;
    private List<ProviderShare> revenueByProvider;
    private List<TopTourRevenue> topToursByRevenue;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyRevenuePoint {
        /** ISO YYYY-MM. */
        private String month;
        private String label;
        private BigDecimal revenue;
        private BigDecimal refund;
        private BigDecimal net;
        private long transactionCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProviderShare {
        private String provider;
        private BigDecimal total;
        private long count;
        private double percent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopTourRevenue {
        private String tourId;
        private String tourTitle;
        private String tourSlug;
        private BigDecimal totalRevenue;
        private long bookingCount;
    }
}
