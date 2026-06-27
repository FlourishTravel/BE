package com.flourishtravel.domain.booking.service;

import com.flourishtravel.common.exception.BadRequestException;
import com.flourishtravel.common.exception.ResourceNotFoundException;
import com.flourishtravel.common.util.UrlUtils;
import com.flourishtravel.domain.booking.dto.CreateBookingResponse;
import com.flourishtravel.domain.booking.dto.GuestInputDto;
import com.flourishtravel.domain.booking.dto.MomoPayUrlResponse;
import com.flourishtravel.domain.booking.dto.UserBookingDetailDto;
import com.flourishtravel.domain.booking.dto.UserBookingGuestLineDto;
import com.flourishtravel.domain.booking.dto.UserBookingPaymentLineDto;
import com.flourishtravel.domain.booking.dto.UserBookingRefundLineDto;
import com.flourishtravel.domain.booking.dto.UserBookingSummaryDto;
import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.booking.entity.BookingGuest;
import com.flourishtravel.domain.booking.entity.Promotion;
import com.flourishtravel.domain.booking.repository.BookingGuestRepository;
import com.flourishtravel.domain.booking.repository.BookingRepository;
import com.flourishtravel.domain.booking.repository.PromotionRepository;
import com.flourishtravel.domain.payment.entity.Payment;
import com.flourishtravel.domain.payment.repository.PaymentRepository;
import com.flourishtravel.domain.payment.service.MomoPaymentCompletionService;
import com.flourishtravel.domain.payment.service.MomoPaymentService;
import com.flourishtravel.domain.payment.service.MomoPaymentService.MomoGatewayQueryResult;
import com.flourishtravel.domain.payment.service.PayOSPaymentService;
import com.flourishtravel.domain.payment.entity.Refund;
import com.flourishtravel.domain.payment.repository.RefundRepository;
import com.flourishtravel.domain.tour.entity.Tour;
import com.flourishtravel.domain.tour.entity.TourImage;
import com.flourishtravel.domain.tour.entity.TourSession;
import com.flourishtravel.domain.tour.repository.TourSessionRepository;
import com.flourishtravel.domain.user.entity.User;
import com.flourishtravel.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.model.v2.paymentRequests.PaymentLinkStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final TourSessionRepository sessionRepository;
    private final PromotionRepository promotionRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final BookingGuestRepository bookingGuestRepository;
    private final MomoPaymentService momoPaymentService;
    private final MomoPaymentCompletionService momoPaymentCompletionService;
    private final PayOSPaymentService payOSPaymentService;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    /**
     * Danh sách chuyến đi / đơn đã đặt của khách — payload an toàn cho API công khai (app user).
     */
    @Transactional(readOnly = true)
    public List<UserBookingSummaryDto> listMyBookingSummaries(UUID userId) {
        userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        List<Booking> list = bookingRepository.findWithSummaryGraphByUserId(userId);
        return list.stream().map(this::toUserBookingSummaryDto).toList();
    }

    private UserBookingSummaryDto toUserBookingSummaryDto(Booking b) {
        TourSession session = b.getSession();
        Tour tour = session != null ? session.getTour() : null;
        var category = tour != null ? tour.getCategory() : null;

        Payment latest = latestPayment(b);
        String payStatus = latest != null ? latest.getStatus() : null;
        String orderId = latest != null ? latest.getOrderId() : null;

        boolean refundPending = b.getRefunds() != null && b.getRefunds().stream()
                .anyMatch(r -> r.getStatus() != null && "pending".equalsIgnoreCase(r.getStatus()));

        String customerEmail = b.getUser() != null ? b.getUser().getEmail() : null;

        return UserBookingSummaryDto.builder()
                .bookingId(b.getId())
                .bookingStatus(b.getStatus())
                .guestCount(b.getGuestCount())
                .totalAmount(b.getTotalAmount())
                .discountAmount(b.getDiscountAmount())
                .bookedAt(b.getCreatedAt())
                .sessionId(session != null ? session.getId() : null)
                .sessionStartDate(session != null ? session.getStartDate() : null)
                .sessionEndDate(session != null ? session.getEndDate() : null)
                .sessionStatus(session != null ? session.getStatus() : null)
                .tourId(tour != null ? tour.getId() : null)
                .tourTitle(tour != null ? tour.getTitle() : null)
                .tourSlug(tour != null ? tour.getSlug() : null)
                .tourThumbnailUrl(firstTourThumbnail(tour))
                .tourDurationDays(tour != null ? tour.getDurationDays() : null)
                .tourDurationNights(tour != null ? tour.getDurationNights() : null)
                .categoryName(category != null ? category.getName() : null)
                .customerEmail(customerEmail)
                .paymentStatus(payStatus)
                .paymentOrderId(orderId)
                .refundPending(refundPending)
                .build();
    }

    private static Payment latestPayment(Booking b) {
        if (b.getPayments() == null || b.getPayments().isEmpty()) {
            return null;
        }
        return b.getPayments().stream()
                .max(Comparator.comparing(Payment::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
    }

    private static String firstTourThumbnail(Tour tour) {
        if (tour == null || tour.getImages() == null || tour.getImages().isEmpty()) {
            return null;
        }
        return tour.getImages().stream()
                .sorted(Comparator.comparing(TourImage::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(TourImage::getImageUrl)
                .findFirst()
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public Booking getById(UUID bookingId, UUID userId) {
        Booking b = bookingRepository.findById(bookingId).orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
        if (!b.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Booking", bookingId);
        }
        return b;
    }

    /**
     * Chi tiết đơn cho app khách — DTO đầy đủ, không trả entity JPA.
     */
    @Transactional(readOnly = true)
    public UserBookingDetailDto getMyBookingDetail(UUID bookingId, UUID userId) {
        Booking b = bookingRepository.findDetailForUser(bookingId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
        return toUserBookingDetailDto(b);
    }

    private UserBookingDetailDto toUserBookingDetailDto(Booking b) {
        TourSession session = b.getSession();
        Tour tour = session != null ? session.getTour() : null;
        var category = tour != null ? tour.getCategory() : null;

        Payment latest = latestPayment(b);
        String payStatus = latest != null ? latest.getStatus() : null;
        String orderId = latest != null ? latest.getOrderId() : null;

        boolean refundPending = b.getRefunds() != null && b.getRefunds().stream()
                .anyMatch(r -> r.getStatus() != null && "pending".equalsIgnoreCase(r.getStatus()));

        String customerEmail = b.getUser() != null ? b.getUser().getEmail() : null;
        String customerPhone = b.getUser() != null ? b.getUser().getPhone() : null;

        String guideName = session != null && session.getTourGuide() != null
                ? session.getTourGuide().getFullName()
                : null;

        String promotionCode = b.getPromotion() != null ? b.getPromotion().getCode() : null;

        String continuePaymentUrl = null;
        if (b.getStatus() != null && "pending".equalsIgnoreCase(b.getStatus()) && latest != null
                && latest.getOrderId() != null
                && latest.getStatus() != null && "pending".equalsIgnoreCase(latest.getStatus())) {
            String gatewayFlag = latest.getProvider() != null && "payos".equalsIgnoreCase(latest.getProvider())
                    ? "payos=1" : "momo=1";
            continuePaymentUrl = UrlUtils.joinBaseAndPath(frontendUrl,
                    "/checkout/result?bookingId=" + b.getId() + "&" + gatewayFlag);
        }

        List<UserBookingGuestLineDto> guestLines = b.getBookingGuests() == null ? List.of() :
                b.getBookingGuests().stream()
                        .sorted(Comparator.comparing(BookingGuest::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                        .map(g -> UserBookingGuestLineDto.builder()
                                .guestId(g.getId())
                                .fullName(g.getFullName())
                                .maskedIdNumber(g.getMaskedIdNumber())
                                .dateOfBirth(g.getDateOfBirth())
                                .sortOrder(g.getSortOrder())
                                .build())
                        .toList();

        List<UserBookingPaymentLineDto> payLines = b.getPayments() == null ? List.of() :
                b.getPayments().stream()
                        .sorted(Comparator.comparing(Payment::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                        .map(p -> UserBookingPaymentLineDto.builder()
                                .paymentId(p.getId())
                                .orderId(p.getOrderId())
                                .amount(p.getAmount())
                                .status(p.getStatus())
                                .provider(p.getProvider())
                                .createdAt(p.getCreatedAt())
                                .build())
                        .toList();

        List<UserBookingRefundLineDto> refundLines = b.getRefunds() == null ? List.of() :
                b.getRefunds().stream()
                        .sorted(Comparator.comparing(Refund::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                        .map(r -> UserBookingRefundLineDto.builder()
                                .refundId(r.getId())
                                .amount(r.getAmount())
                                .status(r.getStatus())
                                .reason(r.getReason())
                                .createdAt(r.getCreatedAt())
                                .build())
                        .toList();

        return UserBookingDetailDto.builder()
                .bookingId(b.getId())
                .bookingStatus(b.getStatus())
                .guestCount(b.getGuestCount())
                .totalAmount(b.getTotalAmount())
                .discountAmount(b.getDiscountAmount())
                .bookedAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .sessionId(session != null ? session.getId() : null)
                .sessionStartDate(session != null ? session.getStartDate() : null)
                .sessionEndDate(session != null ? session.getEndDate() : null)
                .sessionStatus(session != null ? session.getStatus() : null)
                .sessionMaxParticipants(session != null ? session.getMaxParticipants() : null)
                .sessionCurrentParticipants(session != null ? session.getCurrentParticipants() : null)
                .tourId(tour != null ? tour.getId() : null)
                .tourTitle(tour != null ? tour.getTitle() : null)
                .tourSlug(tour != null ? tour.getSlug() : null)
                .tourThumbnailUrl(firstTourThumbnail(tour))
                .tourDurationDays(tour != null ? tour.getDurationDays() : null)
                .tourDurationNights(tour != null ? tour.getDurationNights() : null)
                .categoryName(category != null ? category.getName() : null)
                .customerEmail(customerEmail)
                .customerPhone(customerPhone)
                .paymentStatus(payStatus)
                .paymentOrderId(orderId)
                .refundPending(refundPending)
                .promotionCode(promotionCode)
                .specialRequests(b.getSpecialRequests())
                .contactPhone(b.getContactPhone())
                .pickupAddress(b.getPickupAddress())
                .guestNames(b.getGuestNames())
                .emergencyContactName(b.getEmergencyContactName())
                .emergencyContactPhone(b.getEmergencyContactPhone())
                .guideName(guideName)
                .continuePaymentUrl(continuePaymentUrl)
                .guests(guestLines)
                .payments(payLines)
                .refunds(refundLines)
                .build();
    }

    @Transactional
    public CreateBookingResponse create(UUID userId, UUID sessionId, int guestCount, String specialRequests,
                                        String promotionCode, String contactPhone, String pickupAddress, List<String> guestNames,
                                        List<GuestInputDto> guests, String emergencyContactName, String emergencyContactPhone,
                                        String paymentMethod) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        TourSession session = sessionRepository.findById(sessionId).orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));
        assertNoOverlappingActiveTrip(userId, session);
        if (!"scheduled".equals(session.getStatus())) {
            throw new BadRequestException("Lịch này không còn mở đặt");
        }
        int available = session.getMaxParticipants() - session.getCurrentParticipants();
        if (guestCount <= 0 || guestCount > available) {
            throw new BadRequestException("Số chỗ không hợp lệ hoặc đã hết chỗ (còn " + available + ")");
        }
        BigDecimal unitPrice = session.getTour().getBasePrice() != null ? session.getTour().getBasePrice() : BigDecimal.ZERO;
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(guestCount));
        BigDecimal discountAmount = BigDecimal.ZERO;
        Promotion promotion = null;
        if (promotionCode != null && !promotionCode.isBlank()) {
            promotion = findActivePromotion(promotionCode).orElse(null);
            if (promotion != null && Instant.now().isAfter(promotion.getValidFrom()) && Instant.now().isBefore(promotion.getValidTo())) {
                if (promotion.getMinOrderAmount() == null || totalAmount.compareTo(promotion.getMinOrderAmount()) >= 0) {
                    BigDecimal discount = computePromotionDiscount(promotion, totalAmount);
                    discountAmount = discount;
                    totalAmount = totalAmount.subtract(discountAmount);
                }
            } else {
                promotion = null;
            }
        }
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) totalAmount = BigDecimal.ZERO;

        String contact = (contactPhone != null && !contactPhone.isBlank()) ? contactPhone.trim() : user.getPhone();
        String pickup = (pickupAddress != null && !pickupAddress.isBlank()) ? pickupAddress.trim() : null;
        String guestNamesStr;
        if (guests != null && !guests.isEmpty()) {
            guestNamesStr = guests.stream()
                    .map(GuestInputDto::getFullName)
                    .filter(n -> n != null && !n.isBlank())
                    .map(String::trim)
                    .collect(Collectors.joining(", "));
        } else {
            guestNamesStr = (guestNames != null && !guestNames.isEmpty())
                    ? guestNames.stream().filter(n -> n != null && !n.isBlank()).map(String::trim).collect(Collectors.joining(", "))
                    : null;
        }
        String emergencyName = (emergencyContactName != null && !emergencyContactName.isBlank()) ? emergencyContactName.trim() : null;
        String emergencyPhone = (emergencyContactPhone != null && !emergencyContactPhone.isBlank()) ? emergencyContactPhone.trim() : null;

        Booking booking = Booking.builder()
                .user(user)
                .session(session)
                .totalAmount(totalAmount)
                .guestCount(guestCount)
                .specialRequests(specialRequests)
                .contactPhone(contact)
                .pickupAddress(pickup)
                .guestNames(guestNamesStr)
                .emergencyContactName(emergencyName)
                .emergencyContactPhone(emergencyPhone)
                .status("pending")
                .promotion(promotion)
                .discountAmount(discountAmount)
                .build();
        booking = bookingRepository.save(booking);

        if (guests != null && !guests.isEmpty()) {
            for (int i = 0; i < guests.size(); i++) {
                GuestInputDto g = guests.get(i);
                if (g.getFullName() == null || g.getFullName().isBlank()) continue;
                BookingGuest bg = BookingGuest.builder()
                        .booking(booking)
                        .fullName(g.getFullName().trim())
                        .idNumber(g.getIdNumber() != null && !g.getIdNumber().isBlank() ? g.getIdNumber().trim() : null)
                        .dateOfBirth(g.getDateOfBirth())
                        .sortOrder(i)
                        .build();
                bg = bookingGuestRepository.save(bg);
                booking.getBookingGuests().add(bg);
            }
        }

        session.setCurrentParticipants(session.getCurrentParticipants() + guestCount);
        sessionRepository.save(session);
        if (promotion != null) {
            promotion.setUsedCount(promotion.getUsedCount() + 1);
            promotionRepository.save(promotion);
        }

        String orderId = "FT-" + booking.getId().toString().substring(0, 8);
        String requestId = UUID.randomUUID().toString();
        long amountVnd = booking.getTotalAmount().setScale(0, RoundingMode.HALF_UP).longValue();

        String pm = paymentMethod == null || paymentMethod.isBlank() ? "ewallet" : paymentMethod.trim().toLowerCase();
        boolean usePayOS = "payos".equals(pm);
        Long payosOrderCode = null;
        if (usePayOS) {
            payosOrderCode = payOSPaymentService.generateOrderCode();
        }

        Payment payment = Payment.builder()
                .booking(booking)
                .orderId(orderId)
                .requestId(requestId)
                .amount(booking.getTotalAmount())
                .status("pending")
                .provider(usePayOS ? "payos" : "momo")
                .partnerCode(usePayOS ? String.valueOf(payosOrderCode) : null)
                .build();
        paymentRepository.save(payment);

        String paymentUrl = resolveCheckoutPaymentUrl(booking.getId(), orderId, amountVnd, requestId, pm, payosOrderCode);
        return CreateBookingResponse.builder()
                .bookingId(booking.getId())
                .orderId(orderId)
                .paymentUrl(paymentUrl)
                .expiresInSeconds(15 * 60)
                .build();
    }

    /**
     * Lấy lại link thanh toán MoMo cho đơn đang pending (ví dụ sau khi mở link "Thanh toán ngay").
     */
    @Transactional
    public MomoPayUrlResponse resumeMomoPaymentUrl(UUID bookingId, UUID userId) {
        Booking b = getById(bookingId, userId);
        if (!"pending".equalsIgnoreCase(b.getStatus())) {
            throw new BadRequestException("Đơn không còn chờ thanh toán");
        }
        Payment p = latestPayment(b);
        if (p == null || p.getOrderId() == null || p.getOrderId().isBlank()) {
            throw new BadRequestException("Không tìm thấy giao dịch thanh toán");
        }
        if (p.getStatus() != null && !"pending".equalsIgnoreCase(p.getStatus())) {
            throw new BadRequestException("Giao dịch không còn ở trạng thái chờ thanh toán");
        }
        if (!momoPaymentService.isConfigured()) {
            throw new BadRequestException("Chưa cấu hình MoMo (MOMO_PARTNER_CODE, MOMO_ACCESS_KEY, MOMO_SECRET_KEY)");
        }
        String requestId = UUID.randomUUID().toString();
        p.setRequestId(requestId);
        paymentRepository.save(p);
        long amountVnd = p.getAmount().setScale(0, RoundingMode.HALF_UP).longValue();
        String payUrl = momoPaymentService.createPaymentUrl(
                p.getOrderId(), amountVnd, "FlourishTravel " + p.getOrderId(), requestId);
        return MomoPayUrlResponse.builder().paymentUrl(payUrl).build();
    }

    /**
     * Sau khi MoMo redirect về (resultCode=0 trên URL): tra cứu server MoMo rồi cập nhật booking/payment.
     * Bắt buộc khi IPN không gọi được tới BE (localhost, firewall).
     */
    @Transactional
    public void syncMomoPaymentAfterReturn(UUID userId, String orderId) {
        if (orderId == null || orderId.isBlank()) {
            throw new BadRequestException("Thiếu orderId");
        }
        String oid = orderId.trim();
        Payment p = paymentRepository.findByOrderId(oid)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", oid));
        Booking b = p.getBooking();
        if (!b.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Payment", oid);
        }
        if ("paid".equalsIgnoreCase(b.getStatus())) {
            return;
        }
        if (!momoPaymentService.isConfigured()) {
            throw new BadRequestException("Chưa cấu hình MoMo trên server");
        }
        MomoGatewayQueryResult q = momoPaymentService.queryTransactionStatus(oid);
        if (q.resultCode() != 0) {
            log.info("MoMo query orderId={} resultCode={} message={}", oid, q.resultCode(), q.message());
            throw new BadRequestException(
                    "MoMo chưa xác nhận thanh toán thành công"
                            + (q.message() != null && !q.message().isBlank() ? ": " + q.message() : " (mã " + q.resultCode() + ")"));
        }
        momoPaymentCompletionService.applyPaidByOrderId(oid, q.transId());
    }

    /**
     * Lấy lại link thanh toán PayOS cho đơn đang pending.
     */
    @Transactional
    public MomoPayUrlResponse resumePayOSPaymentUrl(UUID bookingId, UUID userId) {
        Booking b = getById(bookingId, userId);
        if (!"pending".equalsIgnoreCase(b.getStatus())) {
            throw new BadRequestException("Đơn không còn chờ thanh toán");
        }
        Payment p = latestPayment(b);
        if (p == null || !"payos".equalsIgnoreCase(p.getProvider())) {
            throw new BadRequestException("Đơn không dùng PayOS");
        }
        if (p.getPartnerCode() == null || p.getPartnerCode().isBlank()) {
            throw new BadRequestException("Không tìm thấy mã đơn PayOS");
        }
        if (p.getStatus() != null && !"pending".equalsIgnoreCase(p.getStatus())) {
            throw new BadRequestException("Giao dịch không còn ở trạng thái chờ thanh toán");
        }
        if (!payOSPaymentService.isConfigured()) {
            throw new BadRequestException("Chưa cấu hình PayOS (PAYOS_CLIENT_ID, PAYOS_API_KEY, PAYOS_CHECKSUM_KEY)");
        }
        long orderCode = Long.parseLong(p.getPartnerCode().trim());
        long amountVnd = p.getAmount().setScale(0, RoundingMode.HALF_UP).longValue();
        String payUrl = payOSPaymentService.createPaymentUrl(
                orderCode, amountVnd, "FlourishTravel " + p.getOrderId(),
                p.getOrderId(), bookingId);
        return MomoPayUrlResponse.builder().paymentUrl(payUrl).build();
    }

    /**
     * Sau khi PayOS redirect về: tra cứu trạng thái link thanh toán rồi cập nhật booking/payment.
     */
    @Transactional
    public void syncPayOSPaymentAfterReturn(UUID userId, String orderId, String orderCode) {
        String oid = orderId != null ? orderId.trim() : "";
        if (oid.isBlank()) {
            if (orderCode == null || orderCode.isBlank()) {
                throw new BadRequestException("Thiếu orderId hoặc orderCode");
            }
            Payment byCode = paymentRepository.findByProviderAndPartnerCode("payos", orderCode.trim())
                    .orElseThrow(() -> new ResourceNotFoundException("Payment", orderCode.trim()));
            oid = byCode.getOrderId();
        }
        syncPayOSPaymentAfterReturn(userId, oid);
    }

    @Transactional
    public void syncPayOSPaymentAfterReturn(UUID userId, String orderId) {
        if (orderId == null || orderId.isBlank()) {
            throw new BadRequestException("Thiếu orderId");
        }
        String oid = orderId.trim();
        Payment p = paymentRepository.findByOrderId(oid)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", oid));
        Booking b = p.getBooking();
        if (!b.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Payment", oid);
        }
        if ("paid".equalsIgnoreCase(b.getStatus())) {
            return;
        }
        if (!"payos".equalsIgnoreCase(p.getProvider())) {
            throw new BadRequestException("Giao dịch không phải PayOS");
        }
        if (p.getPartnerCode() == null || p.getPartnerCode().isBlank()) {
            throw new BadRequestException("Thiếu mã đơn PayOS");
        }
        if (!payOSPaymentService.isConfigured()) {
            throw new BadRequestException("Chưa cấu hình PayOS trên server");
        }
        long orderCode = Long.parseLong(p.getPartnerCode().trim());
        var link = payOSPaymentService.getPaymentLink(orderCode);
        PaymentLinkStatus status = link.getStatus();
        if (PaymentLinkStatus.PAID.equals(status)) {
            momoPaymentCompletionService.applyPaidByOrderId(oid, link.getId());
            return;
        }
        if (PaymentLinkStatus.CANCELLED.equals(status) || PaymentLinkStatus.EXPIRED.equals(status)) {
            momoPaymentCompletionService.applyFailedByOrderId(oid);
            throw new BadRequestException("Giao dịch PayOS đã hủy hoặc hết hạn");
        }
        throw new BadRequestException("PayOS chưa xác nhận thanh toán thành công (trạng thái: "
                + (status != null ? status.name() : "unknown") + ")");
    }

    private String resolveCheckoutPaymentUrl(UUID bookingId, String orderId, long amountVnd, String requestId,
                                             String paymentMethod, Long payosOrderCode) {
        if ("payos".equals(paymentMethod)) {
            if (payOSPaymentService.isConfigured() && payosOrderCode != null) {
                return payOSPaymentService.createPaymentUrl(
                        payosOrderCode, amountVnd, "FlourishTravel " + orderId, orderId, bookingId);
            }
            log.warn("PayOS credentials missing — redirecting to /checkout/result without gateway");
            return UrlUtils.joinBaseAndPath(frontendUrl, "/checkout/result?bookingId=" + bookingId + "&pending=1");
        }
        if ("ewallet".equals(paymentMethod)) {
            if (momoPaymentService.isConfigured()) {
                return momoPaymentService.createPaymentUrl(orderId, amountVnd, "FlourishTravel " + orderId, requestId);
            }
            log.warn("MoMo credentials missing — redirecting to /checkout/result without gateway");
            return UrlUtils.joinBaseAndPath(frontendUrl, "/checkout/result?bookingId=" + bookingId + "&pending=1");
        }
        return UrlUtils.joinBaseAndPath(frontendUrl, "/checkout/result?bookingId=" + bookingId + "&method=" + paymentMethod);
    }

    @Transactional
    public void cancel(UUID bookingId, UUID userId) {
        Booking b = getById(bookingId, userId);
        if (!"pending".equals(b.getStatus())) {
            throw new BadRequestException("Chỉ có thể hủy đơn đang chờ thanh toán");
        }
        b.setStatus("cancelled");
        bookingRepository.save(b);
        TourSession session = b.getSession();
        session.setCurrentParticipants(Math.max(0, session.getCurrentParticipants() - b.getGuestCount()));
        sessionRepository.save(session);
    }

    /**
     * Không cho đặt thêm tour có ngày chồng lên chuyến đã đặt mà chuyến đó chưa kết thúc (theo ngày).
     */
    private void assertNoOverlappingActiveTrip(UUID userId, TourSession newSession) {
        LocalDate today = LocalDate.now();
        LocalDate ns = newSession.getStartDate();
        LocalDate ne = newSession.getEndDate();
        if (ns == null || ne == null) {
            return;
        }
        long n = bookingRepository.countActiveBookingsOverlappingDateRange(userId, today, ns, ne);
        if (n > 0) {
            throw new BadRequestException(
                    "Bạn đang có chuyến đi chưa kết thúc trùng ngày với lịch này. "
                            + "Vui lòng chọn tour hoặc ngày khác, hoặc hoàn tất / hủy đơn chuyến hiện tại trước.");
        }
    }

    @Transactional(readOnly = true)
    public ValidatePromoResult validatePromo(String code, UUID sessionId, int guestCount, UUID userIdOrNull) {
        String normalizedCode = normalizePromoCode(code);
        if (normalizedCode == null) {
            return new ValidatePromoResult(false, BigDecimal.ZERO, "Mã không được để trống");
        }
        TourSession session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            return new ValidatePromoResult(false, BigDecimal.ZERO, "Session không tồn tại");
        }
        if (userIdOrNull != null) {
            try {
                assertNoOverlappingActiveTrip(userIdOrNull, session);
            } catch (BadRequestException ex) {
                return new ValidatePromoResult(false, BigDecimal.ZERO, ex.getMessage());
            }
        }
        BigDecimal unitPrice = session.getTour().getBasePrice() != null ? session.getTour().getBasePrice() : BigDecimal.ZERO;
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(guestCount));
        Optional<Promotion> promoOpt = findActivePromotion(normalizedCode);
        if (promoOpt.isEmpty()) {
            return new ValidatePromoResult(false, BigDecimal.ZERO, "Mã không hợp lệ hoặc đã hết hạn");
        }
        Promotion p = promoOpt.get();
        Instant now = Instant.now();
        if (!now.isAfter(p.getValidFrom()) || !now.isBefore(p.getValidTo())) {
            return new ValidatePromoResult(false, BigDecimal.ZERO, "Mã đã hết hạn hoặc chưa có hiệu lực");
        }
        if (p.getUsageLimit() != null && p.getUsedCount() >= p.getUsageLimit()) {
            return new ValidatePromoResult(false, BigDecimal.ZERO, "Mã đã hết lượt sử dụng");
        }
        if (p.getMinOrderAmount() != null && totalAmount.compareTo(p.getMinOrderAmount()) < 0) {
            return new ValidatePromoResult(
                    false,
                    BigDecimal.ZERO,
                    "Đơn tối thiểu " + p.getMinOrderAmount().stripTrailingZeros().toPlainString() + " VND để dùng mã này");
        }
        BigDecimal discount = computePromotionDiscount(p, totalAmount);
        return new ValidatePromoResult(true, discount, "Áp dụng thành công");
    }

    private String normalizePromoCode(String code) {
        if (code == null) {
            return null;
        }
        String trimmed = code.trim();
        return trimmed.isEmpty() ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    private Optional<Promotion> findActivePromotion(String code) {
        String normalized = normalizePromoCode(code);
        if (normalized == null) {
            return Optional.empty();
        }
        return promotionRepository.findByCodeIgnoreCaseAndIsActiveTrue(normalized);
    }

    private boolean isPercentDiscount(String discountType) {
        return "percent".equalsIgnoreCase(discountType == null ? "" : discountType.trim());
    }

    private BigDecimal computePromotionDiscount(Promotion promotion, BigDecimal totalAmount) {
        BigDecimal discount = isPercentDiscount(promotion.getDiscountType())
                ? totalAmount.multiply(promotion.getDiscountValue()).divide(BigDecimal.valueOf(100))
                : promotion.getDiscountValue();
        if (promotion.getMaxDiscountAmount() != null && discount.compareTo(promotion.getMaxDiscountAmount()) > 0) {
            discount = promotion.getMaxDiscountAmount();
        }
        return discount;
    }

    @Transactional
    public Refund requestRefund(UUID bookingId, UUID userId, String reason) {
        Booking b = getById(bookingId, userId);
        if (!"paid".equals(b.getStatus())) {
            throw new BadRequestException("Chỉ có thể yêu cầu hoàn tiền cho đơn đã thanh toán");
        }
        Refund refund = Refund.builder()
                .booking(b)
                .amount(b.getTotalAmount())
                .reason(reason)
                .status("pending")
                .build();
        return refundRepository.save(refund);
    }

    public record ValidatePromoResult(boolean valid, BigDecimal discountAmount, String message) {}

    /**
     * Kiểm tra trước khi chuyển sang checkout: lịch mở, đủ chỗ; nếu có user thì kiểm tra trùng lịch.
     */
    @Transactional(readOnly = true)
    public ValidateSessionResult validateSessionForBooking(UUID tourIdOrNull, UUID sessionId, int guestCount, UUID userIdOrNull) {
        if (sessionId == null) {
            return new ValidateSessionResult(false, "Thiếu lịch khởi hành");
        }
        if (guestCount < 1) {
            return new ValidateSessionResult(false, "Số khách không hợp lệ");
        }
        TourSession session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            return new ValidateSessionResult(false, "Lịch khởi hành không tồn tại");
        }
        if (tourIdOrNull != null && session.getTour() != null && !tourIdOrNull.equals(session.getTour().getId())) {
            return new ValidateSessionResult(false, "Lịch không thuộc tour này");
        }
        if (!"scheduled".equalsIgnoreCase(session.getStatus())) {
            return new ValidateSessionResult(false, "Lịch này không còn mở đặt");
        }
        int maxP = session.getMaxParticipants() != null ? session.getMaxParticipants() : 0;
        int cur = session.getCurrentParticipants() != null ? session.getCurrentParticipants() : 0;
        int available = maxP - cur;
        if (guestCount > available) {
            return new ValidateSessionResult(false, "Không đủ chỗ (còn " + Math.max(0, available) + " khách)");
        }
        if (userIdOrNull != null) {
            try {
                assertNoOverlappingActiveTrip(userIdOrNull, session);
            } catch (BadRequestException ex) {
                return new ValidateSessionResult(false, ex.getMessage());
            }
        }
        return new ValidateSessionResult(true, "OK");
    }

    public record ValidateSessionResult(boolean valid, String message) {}
}
