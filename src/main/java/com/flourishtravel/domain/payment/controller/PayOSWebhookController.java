package com.flourishtravel.domain.payment.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.domain.payment.service.PayOSWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.payos.model.webhooks.WebhookData;

/**
 * Webhook nhận thông báo thanh toán từ PayOS.
 * URL đăng ký tại my.payos.vn hoặc qua API confirm-webhook — trùng PAYOS_WEBHOOK_URL trong .env.
 */
@RestController
@RequestMapping("/payments/payos")
@RequiredArgsConstructor
@Slf4j
public class PayOSWebhookController {

    private final PayOSWebhookService payOSWebhookService;

    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<Void>> webhook(@RequestBody Object payload) {
        log.info("PayOS webhook received");
        try {
            WebhookData data = payOSWebhookService.verifyWebhookBody(payload);
            payOSWebhookService.processWebhook(data);
        } catch (Exception e) {
            log.error("PayOS webhook processing error", e);
            return ResponseEntity.ok(ApiResponse.ok("Received", null));
        }
        return ResponseEntity.ok(ApiResponse.ok("Received", null));
    }
}
