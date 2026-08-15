package com.flourishtravel.domain.payment.service;

import java.time.Instant;

/**
 * Thời gian giữ chỗ khi đơn chờ thanh toán trên cổng (PayOS / MoMo).
 */
public final class PaymentHoldRules {

    public static final int DEFAULT_EXPIRE_SECONDS = 15 * 60;

    private PaymentHoldRules() {
    }

    public static int holdSeconds(int configuredSeconds) {
        return Math.max(300, configuredSeconds);
    }

    public static Instant expiredAtUnix(Instant now, int configuredSeconds) {
        Instant base = now == null ? Instant.now() : now;
        return base.plusSeconds(holdSeconds(configuredSeconds));
    }

    public static boolean isPastHold(Instant createdAt, Instant now, int configuredSeconds) {
        if (createdAt == null || now == null) {
            return false;
        }
        return !createdAt.plusSeconds(holdSeconds(configuredSeconds)).isAfter(now);
    }
}
