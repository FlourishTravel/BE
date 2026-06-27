package com.flourishtravel.domain.payment.service;

import com.flourishtravel.common.exception.BadRequestException;
import com.flourishtravel.common.util.UrlUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.payos.PayOS;
import vn.payos.exception.PayOSException;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.core.ClientOptions;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Khởi tạo thanh toán PayOS (https://payos.vn/docs).
 */
@Service
@Slf4j
public class PayOSPaymentService {

    /** PayOS giới hạn mô tả 25 ký tự. */
    private static final int MAX_DESCRIPTION_LENGTH = 25;

    @Value("${app.payos.client-id:}")
    private String clientId;

    @Value("${app.payos.api-key:}")
    private String apiKey;

    @Value("${app.payos.checksum-key:}")
    private String checksumKey;

    @Value("${app.payos.return-url:}")
    private String returnUrl;

    @Value("${app.payos.cancel-url:}")
    private String cancelUrl;

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank()
                && apiKey != null && !apiKey.isBlank()
                && checksumKey != null && !checksumKey.isBlank();
    }

    /**
     * Mã đơn số duy nhất cho PayOS (orderCode).
     */
    public long generateOrderCode() {
        long base = System.currentTimeMillis() / 1000L;
        int suffix = ThreadLocalRandom.current().nextInt(100, 1000);
        return base * 1000L + suffix;
    }

    /**
     * Tạo link thanh toán PayOS và trả về checkoutUrl.
     */
    public String createPaymentUrl(long orderCode, long amountVnd, String description) {
        if (!isConfigured()) {
            throw new IllegalStateException("PayOS is not configured");
        }
        if (amountVnd <= 0) {
            throw new BadRequestException("Số tiền thanh toán không hợp lệ");
        }

        String safeDescription = truncateDescription(
                description != null && !description.isBlank() ? description.trim() : "Thanh toan don hang");

        String redirectUrl = UrlUtils.squashDuplicateSlashesExceptScheme(
                returnUrl == null ? "" : returnUrl.trim());
        String cancel = UrlUtils.squashDuplicateSlashesExceptScheme(
                cancelUrl == null ? "" : cancelUrl.trim());

        CreatePaymentLinkRequest paymentData = CreatePaymentLinkRequest.builder()
                .orderCode(orderCode)
                .amount(amountVnd)
                .description(safeDescription)
                .returnUrl(redirectUrl)
                .cancelUrl(cancel)
                .build();

        try {
            CreatePaymentLinkResponse response = payOSClient().paymentRequests().create(paymentData);
            String checkoutUrl = response.getCheckoutUrl();
            if (checkoutUrl == null || checkoutUrl.isBlank()) {
                log.warn("PayOS missing checkoutUrl for orderCode={}", orderCode);
                throw new BadRequestException("PayOS không trả về liên kết thanh toán");
            }
            return checkoutUrl;
        } catch (BadRequestException e) {
            throw e;
        } catch (PayOSException e) {
            log.warn("PayOS create failed orderCode={}: {}", orderCode, e.getMessage());
            throw new BadRequestException("PayOS: " + e.getMessage());
        } catch (Exception e) {
            log.error("PayOS create error orderCode={}", orderCode, e);
            throw new BadRequestException("Lỗi khi tạo giao dịch PayOS: " + e.getMessage());
        }
    }

    /**
     * Tra cứu trạng thái link thanh toán (dùng sau redirect khi webhook chưa tới).
     */
    public PaymentLink getPaymentLink(long orderCode) {
        if (!isConfigured()) {
            throw new IllegalStateException("PayOS is not configured");
        }
        try {
            return payOSClient().paymentRequests().get(orderCode);
        } catch (PayOSException e) {
            log.warn("PayOS get payment link orderCode={}: {}", orderCode, e.getMessage());
            throw new BadRequestException("PayOS: " + e.getMessage());
        } catch (Exception e) {
            log.error("PayOS get payment link error orderCode={}", orderCode, e);
            throw new BadRequestException("Lỗi tra cứu PayOS: " + e.getMessage());
        }
    }

    PayOS payOSClient() {
        return new PayOS(
                ClientOptions.builder()
                        .clientId(clientId.trim())
                        .apiKey(apiKey.trim())
                        .checksumKey(checksumKey.trim())
                        .build());
    }

    private static String truncateDescription(String description) {
        if (description.length() <= MAX_DESCRIPTION_LENGTH) {
            return description;
        }
        return description.substring(0, MAX_DESCRIPTION_LENGTH);
    }
}
