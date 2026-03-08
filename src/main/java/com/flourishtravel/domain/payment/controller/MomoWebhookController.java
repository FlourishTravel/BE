package com.flourishtravel.domain.payment.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.domain.payment.service.MomoIpnService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Webhook IPN từ MoMo sau khi khách thanh toán/hủy.
 * Verify signature (HMAC) có thể bổ sung theo tài liệu MoMo.
 */
@RestController
@RequestMapping("/payments/momo")
@RequiredArgsConstructor
@Slf4j
public class MomoWebhookController {

    private final MomoIpnService momoIpnService;

    @PostMapping("/ipn")
    public ResponseEntity<ApiResponse<Void>> ipn(@RequestBody Map<String, Object> payload) {
        log.info("MoMo IPN received: {}", payload);
        try {
            momoIpnService.processIpn(payload);
        } catch (Exception e) {
            log.error("MoMo IPN processing error", e);
            return ResponseEntity.ok(ApiResponse.ok("Received", null));
        }
        return ResponseEntity.ok(ApiResponse.ok("Received", null));
    }
}
