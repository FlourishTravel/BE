package com.flourishtravel.domain.payment.service;

import com.flourishtravel.common.exception.BadRequestException;
import com.flourishtravel.common.util.UrlUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.payos.PayOS;
import vn.payos.exception.PayOSException;
import vn.payos.model.v1.payouts.Payout;
import vn.payos.model.v1.payouts.PayoutApprovalState;
import vn.payos.model.v1.payouts.PayoutRequests;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.v2.paymentRequests.PaymentLinkStatus;
import vn.payos.model.v2.paymentRequests.Transaction;
import vn.payos.core.ClientOptions;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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

    @Value("${app.booking.pending-expire-seconds:" + PaymentHoldRules.DEFAULT_EXPIRE_SECONDS + "}")
    private int pendingExpireSeconds;

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
        return createPaymentUrl(orderCode, amountVnd, description, null, null);
    }

    public String createPaymentUrl(long orderCode, long amountVnd, String description,
                                   String orderId, UUID bookingId) {
        if (!isConfigured()) {
            throw new IllegalStateException("PayOS is not configured");
        }
        if (amountVnd <= 0) {
            throw new BadRequestException("Số tiền thanh toán không hợp lệ");
        }

        String safeDescription = truncateDescription(
                description != null && !description.isBlank() ? description.trim() : "Thanh toan don hang");

        String redirectUrl = withReturnContext(
                UrlUtils.squashDuplicateSlashesExceptScheme(returnUrl == null ? "" : returnUrl.trim()),
                orderId, bookingId);
        String cancel = withReturnContext(
                UrlUtils.squashDuplicateSlashesExceptScheme(cancelUrl == null ? "" : cancelUrl.trim()),
                orderId, bookingId);

        CreatePaymentLinkRequest paymentData = CreatePaymentLinkRequest.builder()
                .orderCode(orderCode)
                .amount(amountVnd)
                .description(safeDescription)
                .returnUrl(redirectUrl)
                .cancelUrl(cancel)
                .expiredAt(PaymentHoldRules.expiredAtUnix(Instant.now(), pendingExpireSeconds).getEpochSecond())
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

    /**
     * Hủy link thanh toán chưa trả. Link đã PAID/CANCELLED thì bỏ qua, không ném lỗi.
     */
    public void cancelPaymentLink(long orderCode, String cancellationReason) {
        if (!isConfigured()) {
            log.warn("PayOS not configured — skip cancel orderCode={}", orderCode);
            return;
        }
        String reason = cancellationReason == null || cancellationReason.isBlank()
                ? "Huy don"
                : cancellationReason.trim();
        if (reason.length() > 50) {
            reason = reason.substring(0, 50);
        }
        try {
            PaymentLink link = payOSClient().paymentRequests().get(orderCode);
            PaymentLinkStatus status = link == null ? null : link.getStatus();
            if (status == PaymentLinkStatus.CANCELLED
                    || status == PaymentLinkStatus.EXPIRED
                    || status == PaymentLinkStatus.PAID) {
                log.info("PayOS skip cancel orderCode={} status={}", orderCode, status);
                return;
            }
            payOSClient().paymentRequests().cancel(orderCode, reason);
            log.info("PayOS cancelled payment link orderCode={}", orderCode);
        } catch (PayOSException e) {
            String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
            if (msg.contains("không thể hủy") || msg.contains("khong the huy")
                    || msg.contains("cancelled") || msg.contains("đã hủy") || msg.contains("da huy")) {
                log.info("PayOS cancel already settled orderCode={}: {}", orderCode, e.getMessage());
                return;
            }
            log.warn("PayOS cancel failed orderCode={}: {}", orderCode, e.getMessage());
            throw new BadRequestException("PayOS hủy link: " + e.getMessage());
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.warn("PayOS cancel error orderCode={}", orderCode, e);
            throw new BadRequestException("Lỗi hủy link PayOS: " + e.getMessage());
        }
    }

    /**
     * Lấy tài khoản người chuyển từ link PayOS (giao dịch đã PAID).
     */
    public Optional<PayOSPayerBank> resolvePayerBank(long orderCode) {
        if (!isConfigured()) {
            return Optional.empty();
        }
        try {
            return extractPayerBank(getPaymentLink(orderCode));
        } catch (Exception e) {
            log.warn("PayOS resolve payer bank orderCode={}: {}", orderCode, e.getMessage());
            return Optional.empty();
        }
    }

    public static Optional<PayOSPayerBank> extractPayerBank(PaymentLink link) {
        if (link == null || link.getTransactions() == null) {
            return Optional.empty();
        }
        List<?> txs = link.getTransactions();
        for (Object raw : txs) {
            if (!(raw instanceof Transaction tx)) {
                continue;
            }
            PayOSPayerBank bank = new PayOSPayerBank(
                    blankToNull(tx.getCounterAccountBankId()),
                    blankToNull(tx.getCounterAccountNumber()),
                    blankToNull(tx.getCounterAccountName()));
            if (bank.isComplete()) {
                return Optional.of(bank);
            }
        }
        return Optional.empty();
    }

    /**
     * Chi hộ hoàn tiền về đúng tài khoản đã thanh toán.
     * @return payout id PayOS
     */
    public String refundViaPayout(String referenceId, long amountVnd, PayOSPayerBank payer, String description) {
        if (!isConfigured()) {
            throw new BadRequestException("Chưa cấu hình PayOS nên không chi hộ hoàn tiền được");
        }
        if (payer == null || !payer.isComplete()) {
            throw new BadRequestException(
                    "Không có số tài khoản người chuyển từ PayOS nên không hoàn tự động được");
        }
        if (amountVnd <= 0) {
            throw new BadRequestException("Số tiền hoàn PayOS không hợp lệ");
        }
        if (referenceId == null || referenceId.isBlank()) {
            throw new BadRequestException("Thiếu mã tham chiếu hoàn tiền");
        }
        String desc = description == null || description.isBlank() ? "Hoan tien" : description.trim();
        if (desc.length() > 50) {
            desc = desc.substring(0, 50);
        }
        PayoutRequests request = PayoutRequests.builder()
                .referenceId(referenceId.trim())
                .amount(amountVnd)
                .description(desc)
                .toBin(payer.bankBin().trim())
                .toAccountNumber(payer.accountNumber().trim())
                .build();
        try {
            Payout payout = payOSClient().payouts().create(request, referenceId.trim());
            if (payout == null || payout.getId() == null || payout.getId().isBlank()) {
                throw new BadRequestException("PayOS chi hộ không trả về mã lệnh");
            }
            PayoutApprovalState state = payout.getApprovalState();
            if (state == PayoutApprovalState.REJECTED
                    || state == PayoutApprovalState.FAILED
                    || state == PayoutApprovalState.CANCELLED) {
                throw new BadRequestException("PayOS chi hộ thất bại (trạng thái: " + state + ")");
            }
            log.info("PayOS payout created id={} ref={} state={} amount={}",
                    payout.getId(), referenceId, state, amountVnd);
            return payout.getId();
        } catch (BadRequestException e) {
            throw e;
        } catch (PayOSException e) {
            log.warn("PayOS payout failed ref={}: {}", referenceId, e.getMessage());
            throw new BadRequestException("PayOS chi hộ hoàn tiền: " + e.getMessage());
        } catch (Exception e) {
            log.error("PayOS payout error ref={}", referenceId, e);
            throw new BadRequestException("Lỗi chi hộ PayOS: " + e.getMessage());
        }
    }

    public static void applyPayerBankIfBlank(com.flourishtravel.domain.payment.entity.Payment payment,
                                            String bankBin, String accountNumber, String accountName) {
        if (payment == null) {
            return;
        }
        if ((payment.getPayerBankBin() == null || payment.getPayerBankBin().isBlank())
                && bankBin != null && !bankBin.isBlank()) {
            payment.setPayerBankBin(bankBin.trim());
        }
        if ((payment.getPayerAccountNumber() == null || payment.getPayerAccountNumber().isBlank())
                && accountNumber != null && !accountNumber.isBlank()) {
            payment.setPayerAccountNumber(accountNumber.trim());
        }
        if ((payment.getPayerAccountName() == null || payment.getPayerAccountName().isBlank())
                && accountName != null && !accountName.isBlank()) {
            payment.setPayerAccountName(accountName.trim());
        }
    }

    public static void applyPayerBankIfBlank(com.flourishtravel.domain.payment.entity.Payment payment,
                                            PayOSPayerBank bank) {
        if (bank == null) {
            return;
        }
        applyPayerBankIfBlank(payment, bank.bankBin(), bank.accountNumber(), bank.accountName());
    }

    public Optional<PayOSPayerBank> storedOrResolvedPayerBank(
            com.flourishtravel.domain.payment.entity.Payment payment) {
        if (payment != null) {
            PayOSPayerBank stored = new PayOSPayerBank(
                    payment.getPayerBankBin(), payment.getPayerAccountNumber(), payment.getPayerAccountName());
            if (stored.isComplete()) {
                return Optional.of(stored);
            }
        }
        if (payment == null || payment.getPartnerCode() == null || payment.getPartnerCode().isBlank()) {
            return Optional.empty();
        }
        try {
            long orderCode = Long.parseLong(payment.getPartnerCode().trim());
            Optional<PayOSPayerBank> fromPayOS = resolvePayerBank(orderCode);
            fromPayOS.ifPresent(bank -> applyPayerBankIfBlank(payment, bank));
            return fromPayOS;
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
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

    private static String withReturnContext(String base, String orderId, UUID bookingId) {
        String url = base == null ? "" : base;
        if (orderId != null && !orderId.isBlank()) {
            url = UrlUtils.appendQueryParam(url, "orderId", orderId.trim());
        }
        if (bookingId != null) {
            url = UrlUtils.appendQueryParam(url, "bookingId", bookingId.toString());
        }
        return url;
    }
}
