package com.flourishtravel.domain.payment.service;

import com.flourishtravel.domain.payment.entity.Payment;
import com.flourishtravel.domain.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.model.webhooks.WebhookData;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayOSWebhookService {

    private final PayOSPaymentService payOSPaymentService;
    private final MomoPaymentCompletionService completionService;
    private final PaymentRepository paymentRepository;

    @Transactional
    public void processWebhook(WebhookData data) {
        if (data == null) {
            log.warn("PayOS webhook: empty data");
            return;
        }
        Long orderCode = data.getOrderCode();
        if (orderCode == null) {
            log.warn("PayOS webhook: missing orderCode");
            return;
        }
        Optional<Payment> paymentOpt = paymentRepository.findByProviderAndPartnerCode("payos", String.valueOf(orderCode));
        if (paymentOpt.isEmpty()) {
            log.warn("PayOS webhook: payment not found for orderCode={}", orderCode);
            return;
        }
        Payment payment = paymentOpt.get();
        String orderId = payment.getOrderId();
        String reference = data.getReference();

        String code = data.getCode();
        if ("00".equals(code)) {
            completionService.applyPaidByOrderId(orderId, reference);
        } else {
            completionService.applyFailedByOrderId(orderId);
        }
    }

    /**
     * Xác minh chữ ký webhook từ PayOS.
     */
    public WebhookData verifyWebhookBody(Object body) {
        try {
            return payOSPaymentService.payOSClient().webhooks().verify(body);
        } catch (Exception e) {
            log.warn("PayOS webhook verification failed: {}", e.getMessage());
            throw e;
        }
    }
}
