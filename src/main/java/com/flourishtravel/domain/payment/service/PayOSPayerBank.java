package com.flourishtravel.domain.payment.service;

/**
 * Tài khoản ngân hàng đối ứng (người đã thanh toán) để PayOS chi hộ hoàn tiền.
 */
public record PayOSPayerBank(String bankBin, String accountNumber, String accountName) {

    public boolean isComplete() {
        return bankBin != null && !bankBin.isBlank()
                && accountNumber != null && !accountNumber.isBlank();
    }
}
