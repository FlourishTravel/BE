package com.flourishtravel.domain.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * 1 dòng giao dịch trong bảng admin Tài Chính (gộp payment + refund).
 *
 * Phân biệt qua field {@code kind}:
 *   - "payment" → 1 lần thu tiền (Payment entity)
 *   - "refund"  → 1 lần hoàn tiền (Refund entity)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRowDto {

    private UUID id;
    /** "payment" | "refund" */
    private String kind;
    /** Mã hiển thị (vd: PMT-XXXXXXXX hoặc RFD-XXXXXXXX). */
    private String code;

    private UUID bookingId;
    private String bookingCode;

    private String customerName;
    private String customerEmail;
    private String customerPhone;

    private String tourTitle;
    private String tourSlug;

    private BigDecimal amount;
    private BigDecimal feeAmount;
    private BigDecimal netAmount;
    private String currency;

    /** Với payment: pending|paid|failed|refunded — Với refund: pending|processed|rejected. */
    private String status;

    /** Provider (chỉ payment), hoặc null với refund. */
    private String provider;

    /** Hiển thị nhãn loại giao dịch: "Thanh toán đủ", "Cọc", "Hoàn tiền", "Hoàn cọc"... */
    private String typeLabel;

    private Instant createdAt;
    private Instant paidAt;
}
