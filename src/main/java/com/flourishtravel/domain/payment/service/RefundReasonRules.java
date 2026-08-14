package com.flourishtravel.domain.payment.service;

import com.flourishtravel.common.exception.BadRequestException;

/**
 * Lý do hủy / hoàn tiền bắt buộc đủ rõ để admin xác nhận và PayOS chi hộ.
 */
public final class RefundReasonRules {

    public static final int MIN_LENGTH = 8;

    private RefundReasonRules() {
    }

    public static String requireValid(String reason, String actionLabel) {
        if (reason == null || reason.isBlank()) {
            throw new BadRequestException("Vui lòng nhập lý do " + actionLabel);
        }
        String trimmed = reason.trim();
        if (trimmed.length() < MIN_LENGTH) {
            throw new BadRequestException(
                    "Lý do " + actionLabel + " cần ít nhất " + MIN_LENGTH + " ký tự, mô tả rõ ràng");
        }
        return trimmed;
    }
}
