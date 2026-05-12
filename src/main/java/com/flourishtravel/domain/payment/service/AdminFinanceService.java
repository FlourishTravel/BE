package com.flourishtravel.domain.payment.service;

import com.flourishtravel.common.exception.BadRequestException;
import com.flourishtravel.common.exception.ResourceNotFoundException;
import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.booking.repository.BookingRepository;
import com.flourishtravel.domain.payment.dto.FinanceOverviewDto;
import com.flourishtravel.domain.payment.dto.TransactionDetailDto;
import com.flourishtravel.domain.payment.dto.TransactionRowDto;
import com.flourishtravel.domain.payment.dto.UpdatePaymentRequest;
import com.flourishtravel.domain.payment.entity.Payment;
import com.flourishtravel.domain.payment.entity.Refund;
import com.flourishtravel.domain.payment.repository.PaymentRepository;
import com.flourishtravel.domain.payment.repository.RefundRepository;
import com.flourishtravel.domain.tour.entity.Tour;
import com.flourishtravel.domain.tour.entity.TourSession;
import com.flourishtravel.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Service xử lý logic Tài Chính cho admin:
 *   - Overview (stat cards + chart data)
 *   - Liệt kê / tìm kiếm transactions (gộp payment + refund)
 *   - Chi tiết 1 transaction kèm context booking
 *   - Cập nhật payment (note, status thủ công, fee)
 *
 * Lưu ý nghiệp vụ:
 *   - "Doanh thu" = tổng payment paid (KHÔNG trừ refund).
 *   - "Doanh thu ròng" = doanh thu - đã hoàn - fee.
 *   - "Đang chờ thu" = số dư phải thu của booking chưa hoàn thành (sumPendingDeposit).
 *   - Đổi status payment thủ công sang 'paid' sẽ tự gán paidAt = now.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminFinanceService {

    private static final ZoneId ZONE_VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MM/yyyy");
    private static final int CHART_MONTHS = 6;
    private static final int TOP_TOURS_LIMIT = 5;

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final BookingRepository bookingRepository;

    // ---------------- OVERVIEW ----------------

    @Transactional(readOnly = true)
    public FinanceOverviewDto overview() {
        YearMonth thisMonth = YearMonth.now(ZONE_VN);
        YearMonth prevMonth = thisMonth.minusMonths(1);

        Instant nowMonthStart = monthStart(thisMonth);
        Instant nowMonthEnd = monthEnd(thisMonth);
        Instant prevMonthStart = monthStart(prevMonth);
        Instant prevMonthEnd = monthEnd(prevMonth);

        BigDecimal totalRevenue = safe(paymentRepository.sumAllPaid());
        BigDecimal monthlyRev = safe(paymentRepository.sumPaidBetween(nowMonthStart, nowMonthEnd));
        BigDecimal prevMonthlyRev = safe(paymentRepository.sumPaidBetween(prevMonthStart, prevMonthEnd));
        BigDecimal refundedAll = safe(refundRepository.sumAllProcessed());
        BigDecimal pendingRefund = safe(refundRepository.sumPending());
        BigDecimal totalFees = safe(paymentRepository.sumTotalFees());
        BigDecimal pendingCollection = safe(bookingRepository.sumPendingDeposit());

        BigDecimal netRevenue = totalRevenue.subtract(refundedAll).subtract(totalFees);

        double change = computeChangePercent(prevMonthlyRev, monthlyRev);

        // Success rate (tháng này)
        long totalTx = paymentRepository.countByCreatedAtBetween(nowMonthStart, nowMonthEnd);
        long paidTx = paymentRepository.countByStatusAndCreatedAtBetween("paid", nowMonthStart, nowMonthEnd);
        double successRate = totalTx == 0 ? 0.0 : Math.round((paidTx * 100.0 / totalTx) * 10.0) / 10.0;

        BigDecimal avgTx = paidTx == 0
                ? BigDecimal.ZERO
                : monthlyRev.divide(BigDecimal.valueOf(paidTx), 0, RoundingMode.HALF_UP);

        // Chart: 6 tháng gần nhất.
        List<FinanceOverviewDto.MonthlyRevenuePoint> chart = new ArrayList<>();
        for (int i = CHART_MONTHS - 1; i >= 0; i--) {
            YearMonth ym = thisMonth.minusMonths(i);
            Instant from = monthStart(ym);
            Instant to = monthEnd(ym);
            BigDecimal rev = safe(paymentRepository.sumPaidBetween(from, to));
            BigDecimal ref = safe(refundRepository.sumProcessedBetween(from, to));
            long cnt = paymentRepository.countByStatusAndCreatedAtBetween("paid", from, to);
            chart.add(FinanceOverviewDto.MonthlyRevenuePoint.builder()
                    .month(String.format("%04d-%02d", ym.getYear(), ym.getMonthValue()))
                    .label(ym.format(MONTH_LABEL))
                    .revenue(rev)
                    .refund(ref)
                    .net(rev.subtract(ref))
                    .transactionCount(cnt)
                    .build());
        }

        // Phân bổ theo provider.
        List<Object[]> providerAgg = paymentRepository.aggregateByProvider();
        BigDecimal providerTotal = providerAgg.stream()
                .map(arr -> (BigDecimal) arr[1])
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<FinanceOverviewDto.ProviderShare> providers = providerAgg.stream()
                .map(arr -> {
                    String provider = (String) arr[0];
                    BigDecimal total = (BigDecimal) arr[1];
                    long count = ((Number) arr[2]).longValue();
                    double percent = providerTotal.signum() == 0
                            ? 0.0
                            : Math.round(total.divide(providerTotal, 4, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100)).doubleValue() * 10.0) / 10.0;
                    return FinanceOverviewDto.ProviderShare.builder()
                            .provider(provider == null ? "unknown" : provider)
                            .total(total == null ? BigDecimal.ZERO : total)
                            .count(count)
                            .percent(percent)
                            .build();
                })
                .toList();

        // Top tours.
        List<Object[]> topAgg = paymentRepository.topToursByRevenue(PageRequest.of(0, TOP_TOURS_LIMIT));
        List<FinanceOverviewDto.TopTourRevenue> topTours = topAgg.stream()
                .map(arr -> FinanceOverviewDto.TopTourRevenue.builder()
                        .tourId(arr[0] == null ? null : arr[0].toString())
                        .tourTitle((String) arr[1])
                        .tourSlug((String) arr[2])
                        .totalRevenue(arr[3] == null ? BigDecimal.ZERO : (BigDecimal) arr[3])
                        .bookingCount(arr[4] == null ? 0L : ((Number) arr[4]).longValue())
                        .build())
                .toList();

        return FinanceOverviewDto.builder()
                .totalRevenue(totalRevenue)
                .monthlyRevenue(monthlyRev)
                .previousMonthRevenue(prevMonthlyRev)
                .monthlyChangePercent(change)
                .refundedAmount(refundedAll)
                .pendingRefundAmount(pendingRefund)
                .netRevenue(netRevenue)
                .totalFees(totalFees)
                .pendingCollection(pendingCollection)
                .transactionsThisMonth(totalTx)
                .successRatePercent(successRate)
                .averageTransactionValue(avgTx)
                .revenueByMonth(chart)
                .revenueByProvider(providers)
                .topToursByRevenue(topTours)
                .build();
    }

    // ---------------- LIST ----------------

    @Transactional(readOnly = true)
    public Page<TransactionRowDto> listTransactions(String q, String kind, String status, String provider,
                                                    Instant from, Instant to, Pageable pageable) {
        String term = (q == null) ? "" : q.trim().toLowerCase(Locale.ROOT);
        String pattern = "%" + term + "%";
        String normStatus = blankToNull(status);
        String normProvider = blankToNull(provider);
        String normKind = blankToNull(kind);

        // Tìm theo "mã giao dịch" (PMT-/RFD-) bằng cách so prefix UUID nếu user gõ PMT-XXXXXXXX.
        boolean codeMatch = false;
        String idPrefix = "";
        if (term.startsWith("pmt-") || term.startsWith("rfd-")) {
            idPrefix = term.substring(4).replaceAll("[^a-f0-9]", "");
            codeMatch = !idPrefix.isEmpty();
        }

        List<TransactionRowDto> all = new ArrayList<>();

        // PAYMENTS
        if (normKind == null || "payment".equalsIgnoreCase(normKind)) {
            Page<Payment> payments = paymentRepository.adminSearch(normStatus, normProvider, pattern, from, to,
                    PageRequest.of(0, Integer.MAX_VALUE));
            for (Payment p : payments.getContent()) {
                if (codeMatch && !p.getId().toString().replace("-", "").toLowerCase(Locale.ROOT).startsWith(idPrefix)) {
                    continue;
                }
                all.add(toRow(p));
            }
        }

        // REFUNDS — chỉ khi kind = refund hoặc không filter provider/status (vì refund không có provider).
        if (normKind == null || "refund".equalsIgnoreCase(normKind)) {
            if (normProvider == null) {
                String refundStatus = normalizeRefundStatus(normStatus);
                Page<Refund> refunds = refundRepository.adminSearch(refundStatus, pattern, from, to,
                        PageRequest.of(0, Integer.MAX_VALUE));
                for (Refund r : refunds.getContent()) {
                    if (codeMatch && !r.getId().toString().replace("-", "").toLowerCase(Locale.ROOT).startsWith(idPrefix)) {
                        continue;
                    }
                    all.add(toRow(r));
                }
            }
        }

        // Sort by createdAt DESC.
        all.sort(Comparator.comparing(TransactionRowDto::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));

        // Manual pagination.
        int total = all.size();
        int offset = (int) Math.min(pageable.getOffset(), total);
        int end = Math.min(offset + pageable.getPageSize(), total);
        return new PageImpl<>(all.subList(offset, end), pageable, total);
    }

    // ---------------- DETAIL ----------------

    @Transactional(readOnly = true)
    public TransactionDetailDto detail(String kind, UUID id) {
        if ("payment".equalsIgnoreCase(kind)) {
            Payment p = paymentRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Payment", id));
            return toDetail(p);
        }
        if ("refund".equalsIgnoreCase(kind)) {
            Refund r = refundRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Refund", id));
            return toDetail(r);
        }
        throw new BadRequestException("kind phải là 'payment' hoặc 'refund'");
    }

    // ---------------- UPDATE PAYMENT ----------------

    @Transactional
    public TransactionDetailDto updatePayment(UUID paymentId, UpdatePaymentRequest req) {
        Payment p = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));

        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            String prev = p.getStatus();
            String next = req.getStatus().trim().toLowerCase(Locale.ROOT);
            if (!next.equals(prev)) {
                p.setStatus(next);
                if ("paid".equals(next) && p.getPaidAt() == null) {
                    p.setPaidAt(Instant.now());
                }
                if ("failed".equals(next)) {
                    // ép có lý do
                    if ((req.getFailureReason() == null || req.getFailureReason().isBlank())
                            && (p.getFailureReason() == null || p.getFailureReason().isBlank())) {
                        throw new BadRequestException("Cần nhập lý do khi đánh dấu giao dịch thất bại");
                    }
                }
            }
        }

        if (req.getFeeAmount() != null) {
            if (req.getFeeAmount().signum() < 0) {
                throw new BadRequestException("Phí giao dịch không thể âm");
            }
            p.setFeeAmount(req.getFeeAmount());
        }
        if (req.getFailureReason() != null) {
            p.setFailureReason(req.getFailureReason().isBlank() ? null : req.getFailureReason().trim());
        }
        if (req.getAdminNote() != null) {
            p.setAdminNote(req.getAdminNote().isBlank() ? null : req.getAdminNote().trim());
        }

        paymentRepository.save(p);
        return toDetail(p);
    }

    // ---------------- Mapping ----------------

    private TransactionRowDto toRow(Payment p) {
        Booking b = p.getBooking();
        User u = b == null ? null : b.getUser();
        TourSession s = b == null ? null : b.getSession();
        Tour t = s == null ? null : s.getTour();
        BigDecimal fee = p.getFeeAmount() == null ? BigDecimal.ZERO : p.getFeeAmount();
        BigDecimal net = p.getAmount() == null ? BigDecimal.ZERO : p.getAmount().subtract(fee);

        return TransactionRowDto.builder()
                .id(p.getId())
                .kind("payment")
                .code(buildPaymentCode(p.getId()))
                .bookingId(b == null ? null : b.getId())
                .bookingCode(b == null ? null : buildBookingCode(b.getId()))
                .customerName(u == null ? null : u.getFullName())
                .customerEmail(u == null ? null : u.getEmail())
                .customerPhone(u == null ? null : u.getPhone())
                .tourTitle(t == null ? null : t.getTitle())
                .tourSlug(t == null ? null : t.getSlug())
                .amount(p.getAmount())
                .feeAmount(fee)
                .netAmount(net)
                .currency(p.getCurrency() == null ? "VND" : p.getCurrency())
                .status(p.getStatus())
                .provider(p.getProvider())
                .typeLabel(buildPaymentTypeLabel(p, b))
                .createdAt(p.getCreatedAt())
                .paidAt(p.getPaidAt())
                .build();
    }

    private TransactionRowDto toRow(Refund r) {
        Booking b = r.getBooking();
        User u = b == null ? null : b.getUser();
        TourSession s = b == null ? null : b.getSession();
        Tour t = s == null ? null : s.getTour();

        return TransactionRowDto.builder()
                .id(r.getId())
                .kind("refund")
                .code(buildRefundCode(r.getId()))
                .bookingId(b == null ? null : b.getId())
                .bookingCode(b == null ? null : buildBookingCode(b.getId()))
                .customerName(u == null ? null : u.getFullName())
                .customerEmail(u == null ? null : u.getEmail())
                .customerPhone(u == null ? null : u.getPhone())
                .tourTitle(t == null ? null : t.getTitle())
                .tourSlug(t == null ? null : t.getSlug())
                .amount(r.getAmount())
                .feeAmount(BigDecimal.ZERO)
                .netAmount(r.getAmount() == null ? BigDecimal.ZERO : r.getAmount().negate())
                .currency("VND")
                .status(r.getStatus())
                .provider(null)
                .typeLabel(buildRefundTypeLabel(r))
                .createdAt(r.getCreatedAt())
                .paidAt(r.getProcessedAt())
                .build();
    }

    private TransactionDetailDto toDetail(Payment p) {
        Booking b = p.getBooking();
        TransactionDetailDto.BookingContext ctx = buildBookingContext(b);
        BigDecimal fee = p.getFeeAmount() == null ? BigDecimal.ZERO : p.getFeeAmount();
        BigDecimal net = p.getAmount() == null ? BigDecimal.ZERO : p.getAmount().subtract(fee);

        return TransactionDetailDto.builder()
                .id(p.getId())
                .kind("payment")
                .code(buildPaymentCode(p.getId()))
                .provider(p.getProvider())
                .partnerCode(p.getPartnerCode())
                .orderId(p.getOrderId())
                .requestId(p.getRequestId())
                .providerTransId(p.getProviderTransId())
                .signature(p.getSignature())
                .amount(p.getAmount())
                .feeAmount(fee)
                .netAmount(net)
                .currency(p.getCurrency() == null ? "VND" : p.getCurrency())
                .status(p.getStatus())
                .typeLabel(buildPaymentTypeLabel(p, b))
                .createdAt(p.getCreatedAt())
                .paidAt(p.getPaidAt())
                .processedAt(null)
                .processedByName(null)
                .reason(null)
                .failureReason(p.getFailureReason())
                .adminNote(p.getAdminNote())
                .booking(ctx)
                .relatedPayments(b == null ? List.of() : b.getPayments().stream()
                        .map(this::toRelatedPayment).toList())
                .relatedRefunds(b == null ? List.of() : b.getRefunds().stream()
                        .map(this::toRelatedRefund).toList())
                .build();
    }

    private TransactionDetailDto toDetail(Refund r) {
        Booking b = r.getBooking();
        TransactionDetailDto.BookingContext ctx = buildBookingContext(b);

        return TransactionDetailDto.builder()
                .id(r.getId())
                .kind("refund")
                .code(buildRefundCode(r.getId()))
                .provider(null)
                .amount(r.getAmount())
                .feeAmount(BigDecimal.ZERO)
                .netAmount(r.getAmount() == null ? BigDecimal.ZERO : r.getAmount().negate())
                .currency("VND")
                .status(r.getStatus())
                .typeLabel(buildRefundTypeLabel(r))
                .createdAt(r.getCreatedAt())
                .paidAt(null)
                .processedAt(r.getProcessedAt())
                .processedByName(r.getProcessedBy() == null ? null : r.getProcessedBy().getFullName())
                .reason(r.getReason())
                .failureReason(null)
                .adminNote(null)
                .providerTransId(r.getProviderRefundId())
                .booking(ctx)
                .relatedPayments(b == null ? List.of() : b.getPayments().stream()
                        .map(this::toRelatedPayment).toList())
                .relatedRefunds(b == null ? List.of() : b.getRefunds().stream()
                        .map(this::toRelatedRefund).toList())
                .build();
    }

    private TransactionDetailDto.BookingContext buildBookingContext(Booking b) {
        if (b == null) return null;
        TourSession s = b.getSession();
        Tour t = s == null ? null : s.getTour();
        User u = b.getUser();
        return TransactionDetailDto.BookingContext.builder()
                .id(b.getId())
                .code(buildBookingCode(b.getId()))
                .status(b.getStatus())
                .totalAmount(b.getTotalAmount())
                .discountAmount(b.getDiscountAmount())
                .guestCount(b.getGuestCount())
                .departureDate(s == null ? null : s.getStartDate())
                .customerName(u == null ? null : u.getFullName())
                .customerEmail(u == null ? null : u.getEmail())
                .customerPhone(u == null ? null : u.getPhone())
                .tourTitle(t == null ? null : t.getTitle())
                .tourSlug(t == null ? null : t.getSlug())
                .build();
    }

    private TransactionDetailDto.RelatedPayment toRelatedPayment(Payment p) {
        return TransactionDetailDto.RelatedPayment.builder()
                .id(p.getId())
                .code(buildPaymentCode(p.getId()))
                .provider(p.getProvider())
                .amount(p.getAmount())
                .status(p.getStatus())
                .createdAt(p.getCreatedAt())
                .paidAt(p.getPaidAt())
                .build();
    }

    private TransactionDetailDto.RelatedRefund toRelatedRefund(Refund r) {
        return TransactionDetailDto.RelatedRefund.builder()
                .id(r.getId())
                .code(buildRefundCode(r.getId()))
                .amount(r.getAmount())
                .status(r.getStatus())
                .reason(r.getReason())
                .createdAt(r.getCreatedAt())
                .processedAt(r.getProcessedAt())
                .build();
    }

    // ---------------- Helpers ----------------

    private static String buildPaymentCode(UUID id) {
        if (id == null) return null;
        return "PMT-" + id.toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private static String buildRefundCode(UUID id) {
        if (id == null) return null;
        return "RFD-" + id.toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private static String buildBookingCode(UUID id) {
        if (id == null) return null;
        return "FT-" + id.toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    }

    /** Phân loại 1 payment: Thanh toán đủ vs Cọc. */
    private static String buildPaymentTypeLabel(Payment p, Booking b) {
        if (p == null || p.getAmount() == null) return "Thanh toán";
        if (b == null || b.getTotalAmount() == null) return "Thanh toán";
        if ("failed".equalsIgnoreCase(p.getStatus())) return "Thanh toán lỗi";
        if ("refunded".equalsIgnoreCase(p.getStatus())) return "Đã hoàn";
        BigDecimal pct = p.getAmount().multiply(BigDecimal.valueOf(100))
                .divide(b.getTotalAmount(), 0, RoundingMode.HALF_UP);
        return pct.compareTo(BigDecimal.valueOf(95)) >= 0 ? "Thanh toán đủ" : "Thu cọc";
    }

    private static String buildRefundTypeLabel(Refund r) {
        if (r == null) return "Hoàn tiền";
        String st = r.getStatus() == null ? "" : r.getStatus().toLowerCase(Locale.ROOT);
        return switch (st) {
            case "processed" -> "Đã hoàn tiền";
            case "rejected" -> "Hoàn tiền (từ chối)";
            default -> "Hoàn tiền (chờ duyệt)";
        };
    }

    /** Map FE status sang refund status: paid → processed, failed → rejected. */
    private static String normalizeRefundStatus(String s) {
        if (s == null) return null;
        return switch (s.toLowerCase(Locale.ROOT)) {
            case "paid", "processed" -> "processed";
            case "failed", "rejected" -> "rejected";
            case "pending" -> "pending";
            default -> s;
        };
    }

    private static Instant monthStart(YearMonth ym) {
        return ym.atDay(1).atStartOfDay(ZONE_VN).toInstant();
    }

    private static Instant monthEnd(YearMonth ym) {
        return ym.atEndOfMonth().atTime(23, 59, 59, 999_000_000).atZone(ZONE_VN).toInstant();
    }

    private static BigDecimal safe(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    private static double computeChangePercent(BigDecimal prev, BigDecimal cur) {
        if (prev == null || prev.signum() == 0) {
            return cur != null && cur.signum() > 0 ? 100.0 : 0.0;
        }
        BigDecimal diff = cur.subtract(prev);
        double pct = diff.multiply(BigDecimal.valueOf(100))
                .divide(prev, 4, RoundingMode.HALF_UP).doubleValue();
        return Math.round(pct * 10.0) / 10.0;
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank() || "all".equalsIgnoreCase(s)) ? null : s.trim();
    }
}
