package com.flourishtravel.domain.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MomoIpnService {

    private final MomoPaymentCompletionService completionService;

    @Transactional
    public void processIpn(Map<String, Object> payload) {
        String orderId = stringify(payload.get("orderId"));
        if (orderId == null || orderId.isBlank()) {
            log.warn("MoMo IPN: missing orderId");
            return;
        }
        Integer resultCode = parseResultCodeNullable(payload.get("resultCode"));
        if (resultCode == null) {
            log.warn("MoMo IPN: missing or invalid resultCode for orderId={}", orderId);
            return;
        }
        String transId = stringify(payload.get("transId"));

        if (resultCode == 0) {
            completionService.applyPaidByOrderId(orderId, transId);
        } else {
            completionService.applyFailedByOrderId(orderId);
        }
    }

    private static String stringify(Object o) {
        if (o == null) {
            return null;
        }
        String s = String.valueOf(o).trim();
        return s.isEmpty() ? null : s;
    }

    private static Integer parseResultCodeNullable(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(o).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
