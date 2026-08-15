package com.flourishtravel.domain.payment.service;

import com.flourishtravel.domain.payment.entity.Payment;
import com.flourishtravel.domain.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.v2.paymentRequests.PaymentLinkStatus;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * Khách vào PayOS/MoMo rồi thoát: đơn vẫn pending và giữ chỗ.
 * Sau thời gian giữ chỗ, tra cổng — đã trả thì ghi nhận; chưa trả thì hủy đơn và trả chỗ.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PendingPaymentExpiryService {

    private static final List<String> GATEWAY_PROVIDERS = List.of("payos", "momo");
    private static final int BATCH_SIZE = 30;

    private final PaymentRepository paymentRepository;
    private final PayOSPaymentService payOSPaymentService;
    private final MomoPaymentService momoPaymentService;
    private final MomoPaymentCompletionService completionService;

    @Value("${app.booking.pending-expire-seconds:" + PaymentHoldRules.DEFAULT_EXPIRE_SECONDS + "}")
    private int pendingExpireSeconds;

    @Scheduled(fixedRateString = "${app.booking.pending-expire-check-ms:60000}")
    public void expireAbandonedCheckouts() {
        int hold = PaymentHoldRules.holdSeconds(pendingExpireSeconds);
        Instant cutoff = Instant.now().minusSeconds(hold);
        List<Payment> batch = paymentRepository.findExpiredPendingGatewayPayments(
                GATEWAY_PROVIDERS, cutoff, PageRequest.of(0, BATCH_SIZE));
        if (batch == null || batch.isEmpty()) {
            return;
        }
        for (Payment payment : batch) {
            try {
                settleOne(payment);
            } catch (Exception e) {
                log.warn("Expire pending payment {} failed: {}", payment.getId(), e.getMessage());
            }
        }
    }

    private void settleOne(Payment payment) {
        String provider = payment.getProvider() == null ? "" : payment.getProvider().toLowerCase(Locale.ROOT);
        if ("payos".equals(provider)) {
            settlePayOS(payment);
            return;
        }
        if ("momo".equals(provider)) {
            settleMomo(payment);
        }
    }

    private void settlePayOS(Payment payment) {
        String orderId = payment.getOrderId();
        Long orderCode = parseOrderCode(payment.getPartnerCode());
        if (payOSPaymentService.isConfigured() && orderCode != null) {
            try {
                PaymentLink link = payOSPaymentService.getPaymentLink(orderCode);
                PaymentLinkStatus status = link == null ? null : link.getStatus();
                if (PaymentLinkStatus.PAID.equals(status)) {
                    completionService.applyPaidByOrderId(orderId, link.getId());
                    log.info("Pending PayOS {} was paid — recorded after customer left checkout", orderId);
                    return;
                }
                if (status == PaymentLinkStatus.PENDING) {
                    try {
                        payOSPaymentService.cancelPaymentLink(orderCode, "Het han thanh toan");
                    } catch (Exception e) {
                        log.warn("PayOS cancel expired link {}: {}", orderCode, e.getMessage());
                    }
                }
            } catch (Exception e) {
                if (!looksMissingOnGateway(e)) {
                    log.warn("Skip PayOS expire orderId={}: {}", orderId, e.getMessage());
                    return;
                }
            }
        }
        completionService.applyFailedByOrderId(orderId, "Hết hạn thanh toán: khách không hoàn tất trên PayOS");
        log.info("Cancelled unpaid PayOS booking after hold expired orderId={}", orderId);
    }

    private void settleMomo(Payment payment) {
        String orderId = payment.getOrderId();
        if (momoPaymentService.isConfigured()) {
            try {
                var q = momoPaymentService.queryTransactionStatus(orderId);
                if (q.resultCode() == 0) {
                    completionService.applyPaidByOrderId(orderId, q.transId());
                    log.info("Pending MoMo {} was paid — recorded after customer left checkout", orderId);
                    return;
                }
            } catch (Exception e) {
                log.warn("Skip MoMo expire orderId={}: {}", orderId, e.getMessage());
                return;
            }
        }
        completionService.applyFailedByOrderId(orderId, "Hết hạn thanh toán: khách không hoàn tất trên MoMo");
        log.info("Cancelled unpaid MoMo booking after hold expired orderId={}", orderId);
    }

    private static Long parseOrderCode(String partnerCode) {
        if (partnerCode == null || partnerCode.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(partnerCode.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean looksMissingOnGateway(Exception e) {
        String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase(Locale.ROOT);
        return msg.contains("không tìm") || msg.contains("khong tim") || msg.contains("not found");
    }
}
