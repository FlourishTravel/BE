package com.flourishtravel.domain.booking.service;

import com.flourishtravel.common.exception.BadRequestException;
import com.flourishtravel.common.exception.ResourceNotFoundException;
import com.flourishtravel.domain.booking.dto.AdminBookingDetailDto;
import com.flourishtravel.domain.booking.dto.AdminBookingSummaryDto;
import com.flourishtravel.domain.booking.dto.BookingStatsDto;
import com.flourishtravel.domain.booking.dto.BookingStatusUpdateRequest;
import com.flourishtravel.domain.booking.dto.RefundActionRequest;
import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.booking.entity.BookingGuest;
import com.flourishtravel.domain.booking.entity.Promotion;
import com.flourishtravel.domain.booking.repository.BookingRepository;
import com.flourishtravel.domain.payment.entity.Payment;
import com.flourishtravel.domain.payment.entity.Refund;
import com.flourishtravel.domain.payment.repository.PaymentRepository;
import com.flourishtravel.domain.payment.repository.RefundRepository;
import com.flourishtravel.domain.tour.entity.Tour;
import com.flourishtravel.domain.tour.entity.TourImage;
import com.flourishtravel.domain.tour.entity.TourSession;
import com.flourishtravel.domain.user.entity.User;
import com.flourishtravel.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Business logic phía admin cho Quản Lý Đặt Chỗ.
 *
 * Trách nhiệm:
 *  - Liệt kê / lọc booking, hiển thị tổng hợp thanh toán.
 *  - Đổi trạng thái booking theo state machine.
 *  - Duyệt / từ chối yêu cầu hoàn tiền.
 *  - Đánh dấu thanh toán bằng tay (chuyển khoản).
 *  - Tính stats cho dashboard.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminBookingService {

    private static final ZoneId ZONE_VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final UserRepository userRepository;
    private final SessionParticipantSyncService sessionParticipantSyncService;

    // ---------- Queries ----------

    @Transactional(readOnly = true)
    public Page<AdminBookingSummaryDto> adminList(String q, String status, Instant from, Instant to, Pageable pageable) {
        String term = (q == null) ? "" : q.trim().toLowerCase(Locale.ROOT);
        String pattern = "%" + term + "%";
        String normalizedStatus = (status == null || status.isBlank() || "all".equalsIgnoreCase(status))
                ? null
                : status.trim().toLowerCase(Locale.ROOT);

        // Khi tìm kiếm theo bookingCode "FT-XXXXXXXX": ưu tiên match theo prefix UUID (8 ký tự).
        if (term.startsWith("ft-") && term.length() >= 4) {
            String prefix = term.substring(3).replaceAll("[^a-f0-9]", "");
            if (!prefix.isEmpty()) {
                List<AdminBookingSummaryDto> matched = bookingRepository.findAll().stream()
                        .filter(b -> b.getId() != null
                                && b.getId().toString().toLowerCase(Locale.ROOT).startsWith(prefix))
                        .filter(b -> normalizedStatus == null
                                || normalizedStatus.equalsIgnoreCase(b.getStatus()))
                        .map(this::toSummary)
                        .toList();
                return new PageImpl<>(matched, pageable, matched.size());
            }
        }

        // Lọc đặc biệt "refund_pending": JPQL không có status này; lọc bằng cờ hasRefundPending.
        if ("refund_pending".equals(normalizedStatus)) {
            Page<Booking> page = bookingRepository.adminSearch(null, pattern, from, to, pageable);
            List<AdminBookingSummaryDto> filtered = page.getContent().stream()
                    .map(this::toSummary)
                    .filter(AdminBookingSummaryDto::isHasRefundPending)
                    .toList();
            return new PageImpl<>(filtered, pageable, filtered.size());
        }

        return bookingRepository
                .adminSearch(normalizedStatus, pattern, from, to, pageable)
                .map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public AdminBookingDetailDto adminDetail(UUID id) {
        Booking b = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", id));
        return toDetail(b);
    }

    @Transactional(readOnly = true)
    public BookingStatsDto stats() {
        // Phạm vi tháng hiện tại theo timezone VN.
        LocalDate today = LocalDate.now(ZONE_VN);
        YearMonth ym = YearMonth.from(today);
        Instant from = ym.atDay(1).atStartOfDay(ZONE_VN).toInstant();
        Instant to = ym.atEndOfMonth().plusDays(1).atStartOfDay(ZONE_VN).toInstant();

        long total = bookingRepository.countByCreatedAtBetween(from, to);
        BigDecimal monthlyRevenue = bookingRepository.sumPaidPaymentsBetween(from, to);
        BigDecimal pendingDeposit = bookingRepository.sumPendingDeposit();
        long pendingRefunds = bookingRepository.countPendingRefunds();

        BookingStatsDto.StatusBreakdown breakdown = BookingStatsDto.StatusBreakdown.builder()
                .pending(bookingRepository.countByStatus("pending"))
                .paid(bookingRepository.countByStatus("paid"))
                .confirmed(bookingRepository.countByStatus("confirmed"))
                .completed(bookingRepository.countByStatus("completed"))
                .cancelled(bookingRepository.countByStatus("cancelled"))
                .refundPending(pendingRefunds)
                .build();

        return BookingStatsDto.builder()
                .totalBookings(total)
                .monthlyRevenue(monthlyRevenue != null ? monthlyRevenue : BigDecimal.ZERO)
                .pendingDeposit(pendingDeposit != null ? pendingDeposit : BigDecimal.ZERO)
                .pendingRefundRequests(pendingRefunds)
                .breakdown(breakdown)
                .build();
    }

    // ---------- Mutations ----------

    /**
     * Đổi trạng thái booking theo state machine.
     * Khi chuyển sang "cancelled" tự cộng trả slot session.
     */
    @Transactional
    public AdminBookingDetailDto updateStatus(UUID id, BookingStatusUpdateRequest req) {
        Booking b = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", id));

        String current = b.getStatus() == null ? "pending" : b.getStatus().toLowerCase(Locale.ROOT);
        String target = req.getStatus().toLowerCase(Locale.ROOT);

        if (current.equals(target)) {
            return toDetail(b);
        }

        validateTransition(current, target);

        if ("cancelled".equals(target) && !"cancelled".equals(current)) {
            releaseSeats(b);
        }
        if ("completed".equals(target)) {
            // Đảm bảo đã thanh toán đủ trước khi mark complete (tránh sai lệch tài chính).
            BigDecimal paid = computePaidAmount(b);
            if (paid.compareTo(b.getTotalAmount()) < 0) {
                throw new BadRequestException("Không thể hoàn thành: KH chưa thanh toán đủ");
            }
        }

        b.setStatus(target);
        Booking saved = bookingRepository.save(b);
        if ("paid".equals(target)) {
            sessionParticipantSyncService.syncPaidBooking(saved.getId());
        }
        log.info("[AdminBooking] {} status: {} -> {} (note={})", saved.getId(), current, target, req.getNote());
        return toDetail(saved);
    }

    /**
     * Admin đánh dấu booking đã thanh toán đủ (thường khi nhận chuyển khoản thủ công).
     * Tạo 1 payment record "manual" để minh bạch lịch sử tài chính.
     */
    @Transactional
    public AdminBookingDetailDto markPaid(UUID id, BigDecimal amount, String note) {
        Booking b = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", id));
        if ("cancelled".equals(b.getStatus())) {
            throw new BadRequestException("Booking đã huỷ, không thể đánh dấu thanh toán");
        }

        BigDecimal currentPaid = computePaidAmount(b);
        BigDecimal balance = b.getTotalAmount().subtract(currentPaid);
        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Booking đã thanh toán đủ");
        }

        BigDecimal applied = (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) ? balance : amount;
        if (applied.compareTo(balance) > 0) applied = balance;

        Payment payment = Payment.builder()
                .booking(b)
                .provider("manual")
                .orderId("MAN-" + b.getId().toString().substring(0, 8) + "-" + System.currentTimeMillis())
                .amount(applied)
                .status("paid")
                .signature(note != null ? note.trim() : null)
                .build();
        paymentRepository.save(payment);

        // Nếu đã đủ tiền và đang pending -> chuyển sang paid.
        BigDecimal newPaid = currentPaid.add(applied);
        if (newPaid.compareTo(b.getTotalAmount()) >= 0 && "pending".equals(b.getStatus())) {
            b.setStatus("paid");
            bookingRepository.save(b);
            sessionParticipantSyncService.syncPaidBooking(b.getId());
        }
        return adminDetail(b.getId());
    }

    /** Duyệt 1 refund (status='pending' -> 'approved') và đổi booking sang trạng thái cancelled (nếu chưa). */
    @Transactional
    public AdminBookingDetailDto approveRefund(UUID bookingId, UUID adminUserId, RefundActionRequest req) {
        Booking b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
        Refund refund = pickRefundForAction(b, req == null ? null : req.getRefundId());

        // Chặn duyệt khi đã qua ngày khởi hành (chính sách: cần xử lý sự cố thủ công).
        if (b.getSession() != null && b.getSession().getStartDate() != null
                && b.getSession().getStartDate().isBefore(LocalDate.now(ZONE_VN))) {
            throw new BadRequestException(
                    "Tour đã khởi hành, vui lòng xử lý hoàn tiền theo quy trình thủ công");
        }

        BigDecimal amount = (req != null && req.getAmount() != null && req.getAmount().compareTo(BigDecimal.ZERO) > 0)
                ? req.getAmount()
                : refund.getAmount();
        BigDecimal paid = computePaidAmount(b);
        if (amount.compareTo(paid) > 0) {
            throw new BadRequestException(
                    "Số tiền duyệt hoàn (" + amount + ") vượt quá số đã thanh toán (" + paid + ")");
        }

        refund.setAmount(amount);
        refund.setStatus("approved");
        refund.setProcessedAt(Instant.now());
        if (adminUserId != null) {
            User admin = userRepository.findById(adminUserId).orElse(null);
            refund.setProcessedBy(admin);
        }
        if (req != null && req.getReason() != null && !req.getReason().isBlank()) {
            String existing = refund.getReason() == null ? "" : refund.getReason() + " | ";
            refund.setReason(existing + "Admin: " + req.getReason().trim());
        }
        refundRepository.save(refund);

        // Huỷ booking & release seat khi duyệt hoàn tiền.
        if (!"cancelled".equals(b.getStatus())) {
            releaseSeats(b);
            b.setStatus("cancelled");
            bookingRepository.save(b);
        }

        log.info("[AdminBooking] Refund {} approved amount={} by admin={}", refund.getId(), amount, adminUserId);
        return adminDetail(b.getId());
    }

    /** Từ chối refund (status='pending' -> 'rejected'). */
    @Transactional
    public AdminBookingDetailDto rejectRefund(UUID bookingId, UUID adminUserId, RefundActionRequest req) {
        if (req == null || req.getReason() == null || req.getReason().isBlank()) {
            throw new BadRequestException("Cần cung cấp lý do từ chối");
        }
        Booking b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
        Refund refund = pickRefundForAction(b, req.getRefundId());

        refund.setStatus("rejected");
        refund.setProcessedAt(Instant.now());
        if (adminUserId != null) {
            User admin = userRepository.findById(adminUserId).orElse(null);
            refund.setProcessedBy(admin);
        }
        String existing = refund.getReason() == null ? "" : refund.getReason() + " | ";
        refund.setReason(existing + "Admin từ chối: " + req.getReason().trim());
        refundRepository.save(refund);

        log.info("[AdminBooking] Refund {} rejected by admin={}", refund.getId(), adminUserId);
        return adminDetail(b.getId());
    }

    // ---------- Helpers ----------

    private void validateTransition(String current, String target) {
        boolean allowed = switch (current) {
            case "pending" -> target.equals("paid") || target.equals("cancelled");
            case "paid" -> target.equals("confirmed") || target.equals("cancelled") || target.equals("completed");
            case "confirmed" -> target.equals("completed") || target.equals("cancelled");
            case "completed", "cancelled" -> false;
            default -> true;
        };
        if (!allowed) {
            throw new BadRequestException(
                    "Không thể chuyển trạng thái: " + current + " -> " + target);
        }
    }

    private void releaseSeats(Booking b) {
        TourSession session = b.getSession();
        if (session == null) return;
        int newCount = Math.max(0, (session.getCurrentParticipants() == null ? 0 : session.getCurrentParticipants())
                - (b.getGuestCount() == null ? 0 : b.getGuestCount()));
        session.setCurrentParticipants(newCount);
    }

    private Refund pickRefundForAction(Booking b, UUID refundId) {
        if (b.getRefunds() == null || b.getRefunds().isEmpty()) {
            throw new BadRequestException("Booking không có yêu cầu hoàn tiền nào");
        }
        Refund target;
        if (refundId != null) {
            target = b.getRefunds().stream()
                    .filter(r -> refundId.equals(r.getId()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Refund", refundId));
        } else {
            target = b.getRefunds().stream()
                    .filter(r -> "pending".equalsIgnoreCase(r.getStatus()))
                    .max(Comparator.comparing(Refund::getCreatedAt,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .orElseThrow(() -> new BadRequestException("Không có refund pending"));
        }
        if (!"pending".equalsIgnoreCase(target.getStatus())) {
            throw new BadRequestException("Yêu cầu hoàn tiền đã được xử lý trước đó");
        }
        return target;
    }

    private BigDecimal computePaidAmount(Booking b) {
        if (b.getPayments() == null) return BigDecimal.ZERO;
        return b.getPayments().stream()
                .filter(p -> "paid".equalsIgnoreCase(p.getStatus()))
                .map(p -> p.getAmount() == null ? BigDecimal.ZERO : p.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal computeRefundedAmount(Booking b) {
        if (b.getRefunds() == null) return BigDecimal.ZERO;
        return b.getRefunds().stream()
                .filter(r -> "approved".equalsIgnoreCase(r.getStatus())
                        || "completed".equalsIgnoreCase(r.getStatus()))
                .map(r -> r.getAmount() == null ? BigDecimal.ZERO : r.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String paymentClass(Booking b, BigDecimal paid, BigDecimal refunded) {
        if (refunded != null && refunded.compareTo(BigDecimal.ZERO) > 0) {
            return "refunded";
        }
        if (b.getRefunds() != null && b.getRefunds().stream().anyMatch(r -> "pending".equalsIgnoreCase(r.getStatus()))) {
            return "refund_pending";
        }
        if (b.getTotalAmount() == null) return "unpaid";
        int cmp = paid.compareTo(b.getTotalAmount());
        if (cmp >= 0) return "paid";
        if (paid.compareTo(BigDecimal.ZERO) > 0) return "partial";
        return "unpaid";
    }

    private boolean hasRefundPending(Booking b) {
        return b.getRefunds() != null
                && b.getRefunds().stream().anyMatch(r -> "pending".equalsIgnoreCase(r.getStatus()));
    }

    private AdminBookingSummaryDto toSummary(Booking b) {
        User u = b.getUser();
        TourSession s = b.getSession();
        Tour t = s == null ? null : s.getTour();

        BigDecimal paid = computePaidAmount(b);
        BigDecimal refunded = computeRefundedAmount(b);
        BigDecimal balance = b.getTotalAmount() == null ? BigDecimal.ZERO
                : b.getTotalAmount().subtract(paid).max(BigDecimal.ZERO);

        return AdminBookingSummaryDto.builder()
                .id(b.getId())
                .bookingCode(buildBookingCode(b))
                .status(b.getStatus() == null ? "pending" : b.getStatus().toLowerCase(Locale.ROOT))
                .hasRefundPending(hasRefundPending(b))
                .customer(u == null ? null : AdminBookingSummaryDto.CustomerRef.builder()
                        .id(u.getId())
                        .fullName(u.getFullName())
                        .email(u.getEmail())
                        .phone(u.getPhone())
                        .avatarUrl(u.getAvatarUrl())
                        .build())
                .tour(t == null ? null : AdminBookingSummaryDto.TourRef.builder()
                        .id(t.getId())
                        .title(t.getTitle())
                        .slug(t.getSlug())
                        .tourCode(buildTourCode(t))
                        .thumbnailUrl(pickThumbnail(t))
                        .build())
                .session(s == null ? null : AdminBookingSummaryDto.SessionRef.builder()
                        .id(s.getId())
                        .startDate(s.getStartDate())
                        .endDate(s.getEndDate())
                        .status(s.getStatus())
                        .maxParticipants(s.getMaxParticipants())
                        .currentParticipants(s.getCurrentParticipants())
                        .build())
                .guestCount(b.getGuestCount())
                .totalAmount(b.getTotalAmount())
                .discountAmount(b.getDiscountAmount())
                .paidAmount(paid)
                .balanceAmount(balance)
                .paymentClass(paymentClass(b, paid, refunded))
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .build();
    }

    private AdminBookingDetailDto toDetail(Booking b) {
        AdminBookingSummaryDto summary = toSummary(b);

        BigDecimal paid = computePaidAmount(b);
        BigDecimal refunded = computeRefundedAmount(b);

        LocalDate departure = b.getSession() == null ? null : b.getSession().getStartDate();
        List<AdminBookingDetailDto.GuestRef> guests = b.getBookingGuests() == null ? List.of() :
                b.getBookingGuests().stream()
                        .sorted(Comparator.comparing(BookingGuest::getSortOrder,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                        .map(g -> AdminBookingDetailDto.GuestRef.builder()
                                .id(g.getId())
                                .fullName(g.getFullName())
                                .maskedIdNumber(g.getMaskedIdNumber())
                                .dateOfBirth(g.getDateOfBirth())
                                .ageAtDeparture(calcAge(g.getDateOfBirth(), departure))
                                .sortOrder(g.getSortOrder())
                                .build())
                        .toList();

        List<AdminBookingDetailDto.PaymentRef> payments = b.getPayments() == null ? List.of() :
                b.getPayments().stream()
                        .sorted(Comparator.comparing(Payment::getCreatedAt,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                        .map(p -> AdminBookingDetailDto.PaymentRef.builder()
                                .id(p.getId())
                                .orderId(p.getOrderId())
                                .provider(p.getProvider())
                                .amount(p.getAmount())
                                .status(p.getStatus())
                                .providerTransId(p.getProviderTransId())
                                .createdAt(p.getCreatedAt())
                                .build())
                        .toList();

        List<AdminBookingDetailDto.RefundRef> refunds = b.getRefunds() == null ? List.of() :
                b.getRefunds().stream()
                        .sorted(Comparator.comparing(Refund::getCreatedAt,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                        .map(r -> AdminBookingDetailDto.RefundRef.builder()
                                .id(r.getId())
                                .amount(r.getAmount())
                                .reason(r.getReason())
                                .status(r.getStatus())
                                .processedByName(r.getProcessedBy() == null ? null : r.getProcessedBy().getFullName())
                                .processedAt(r.getProcessedAt())
                                .providerRefundId(r.getProviderRefundId())
                                .createdAt(r.getCreatedAt())
                                .build())
                        .toList();

        Promotion promo = b.getPromotion();
        AdminBookingDetailDto.PromotionRef promoRef = promo == null ? null :
                AdminBookingDetailDto.PromotionRef.builder()
                        .id(promo.getId())
                        .code(promo.getCode())
                        .name(promo.getName())
                        .discountType(promo.getDiscountType())
                        .discountValue(promo.getDiscountValue())
                        .build();

        return AdminBookingDetailDto.builder()
                .id(summary.getId())
                .bookingCode(summary.getBookingCode())
                .status(summary.getStatus())
                .hasRefundPending(summary.isHasRefundPending())
                .customer(summary.getCustomer())
                .tour(summary.getTour())
                .session(summary.getSession())
                .guestCount(summary.getGuestCount())
                .totalAmount(summary.getTotalAmount())
                .discountAmount(summary.getDiscountAmount())
                .paidAmount(paid)
                .balanceAmount(summary.getBalanceAmount())
                .refundedAmount(refunded)
                .paymentClass(summary.getPaymentClass())
                .contactPhone(b.getContactPhone())
                .pickupAddress(b.getPickupAddress())
                .specialRequests(b.getSpecialRequests())
                .emergencyContactName(b.getEmergencyContactName())
                .emergencyContactPhone(b.getEmergencyContactPhone())
                .promotion(promoRef)
                .guests(guests)
                .payments(payments)
                .refunds(refunds)
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .build();
    }

    private String buildBookingCode(Booking b) {
        if (b == null || b.getId() == null) return "";
        return "FT-" + b.getId().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private String buildTourCode(Tour t) {
        if (t == null || t.getSlug() == null || t.getSlug().isBlank()) return "";
        String[] tokens = t.getSlug().split("-");
        String code = Arrays.stream(tokens)
                .filter(s -> !s.isBlank())
                .map(s -> String.valueOf(s.charAt(0)))
                .collect(Collectors.joining());
        if (code.length() > 6) code = code.substring(0, 6);
        return code.toUpperCase(Locale.ROOT);
    }

    private String pickThumbnail(Tour t) {
        if (t.getImages() == null || t.getImages().isEmpty()) return null;
        return t.getImages().stream()
                .sorted(Comparator.comparing(TourImage::getSortOrder,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .findFirst()
                .map(TourImage::getImageUrl)
                .orElse(null);
    }

    private Integer calcAge(LocalDate dob, LocalDate at) {
        if (dob == null) return null;
        LocalDate reference = at == null ? LocalDate.now(ZONE_VN) : at;
        if (dob.isAfter(reference)) return 0;
        return Period.between(dob, reference).getYears();
    }
}
