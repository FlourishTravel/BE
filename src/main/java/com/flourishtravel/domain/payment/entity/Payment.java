package com.flourishtravel.domain.payment.entity;

import com.flourishtravel.common.entity.BaseEntity;
import com.flourishtravel.domain.booking.entity.Booking;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments", indexes = {@Index(columnList = "booking_id"), @Index(columnList = "order_id"), @Index(columnList = "status")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    /** Nhà cung cấp / cổng thanh toán: momo | vnpay | bank_transfer | manual | credit_card ... */
    @Column(nullable = false, length = 50)
    @Builder.Default
    private String provider = "momo";

    @Column(name = "partner_code", length = 50)
    private String partnerCode;

    @Column(name = "order_id", nullable = false, length = 100)
    private String orderId;

    @Column(name = "request_id", length = 100)
    private String requestId;

    @Column(name = "provider_trans_id", length = 100)
    private String providerTransId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    /** Phí giao dịch cổng thanh toán (nếu có), dùng để tính net revenue. */
    @Column(name = "fee_amount", precision = 15, scale = 2)
    private BigDecimal feeAmount;

    /** Loại tiền tệ (mặc định VND). Để mở rộng tour quốc tế trong tương lai. */
    @Column(length = 10, nullable = false)
    @Builder.Default
    private String currency = "VND";

    @Column(length = 500)
    private String signature;

    /** pending | paid | failed | refunded */
    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "pending";

    /** Thời điểm thanh toán thành công — set khi status chuyển sang paid. */
    @Column(name = "paid_at")
    private Instant paidAt;

    /** Lý do giao dịch thất bại (khi status=failed). */
    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    /** Ghi chú nội bộ admin về giao dịch. */
    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;
}
