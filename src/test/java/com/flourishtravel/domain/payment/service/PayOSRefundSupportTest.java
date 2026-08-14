package com.flourishtravel.domain.payment.service;

import com.flourishtravel.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.v2.paymentRequests.PaymentLinkStatus;
import vn.payos.model.v2.paymentRequests.Transaction;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayOSRefundSupportTest {

    @Test
    void extractPayerBank_fromPaidTransaction() {
        Transaction tx = Transaction.builder()
                .reference("TF123")
                .amount(10000L)
                .accountNumber("1133")
                .description("pay")
                .transactionDateTime("2026-08-15 10:00:00")
                .counterAccountBankId("970422")
                .counterAccountNumber("0123456789")
                .counterAccountName("NGUYEN VAN A")
                .build();
        PaymentLink link = PaymentLink.builder()
                .id("link-1")
                .orderCode(1L)
                .amount(10000L)
                .amountPaid(10000L)
                .amountRemaining(0L)
                .status(PaymentLinkStatus.PAID)
                .createdAt("2026-08-15T10:00:00.000Z")
                .transactions(List.of(tx))
                .build();

        var bank = PayOSPaymentService.extractPayerBank(link);
        assertTrue(bank.isPresent());
        assertEquals("970422", bank.get().bankBin());
        assertEquals("0123456789", bank.get().accountNumber());
        assertTrue(bank.get().isComplete());
    }

    @Test
    void extractPayerBank_missingCounterAccount() {
        Transaction tx = Transaction.builder()
                .reference("TF123")
                .amount(10000L)
                .accountNumber("1133")
                .description("pay")
                .transactionDateTime("2026-08-15 10:00:00")
                .build();
        PaymentLink link = PaymentLink.builder()
                .id("link-1")
                .orderCode(1L)
                .amount(10000L)
                .amountPaid(10000L)
                .amountRemaining(0L)
                .status(PaymentLinkStatus.PAID)
                .createdAt("2026-08-15T10:00:00.000Z")
                .transactions(List.of(tx))
                .build();

        assertTrue(PayOSPaymentService.extractPayerBank(link).isEmpty());
    }

    @Test
    void refundReason_requiresClearText() {
        assertThrows(BadRequestException.class, () -> RefundReasonRules.requireValid("  ", "hủy đơn"));
        assertThrows(BadRequestException.class, () -> RefundReasonRules.requireValid("abc", "hủy đơn"));
        assertEquals("Khách đổi lịch trình", RefundReasonRules.requireValid("Khách đổi lịch trình", "hủy đơn"));
    }

    @Test
    void payerBank_incompleteWithoutAccount() {
        assertFalse(new PayOSPayerBank("970422", null, "A").isComplete());
        assertTrue(new PayOSPayerBank("970422", "123", "A").isComplete());
    }
}
