package com.flourishtravel.domain.booking.service;

import com.flourishtravel.common.exception.BadRequestException;
import com.flourishtravel.common.exception.ResourceNotFoundException;
import com.flourishtravel.domain.booking.dto.GuestInputDto;
import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.booking.entity.BookingGuest;
import com.flourishtravel.domain.booking.entity.Promotion;
import com.flourishtravel.domain.booking.repository.BookingGuestRepository;
import com.flourishtravel.domain.booking.repository.BookingRepository;
import com.flourishtravel.domain.booking.repository.PromotionRepository;
import com.flourishtravel.domain.payment.entity.Payment;
import com.flourishtravel.domain.payment.repository.PaymentRepository;
import com.flourishtravel.domain.payment.entity.Refund;
import com.flourishtravel.domain.payment.repository.RefundRepository;
import com.flourishtravel.domain.tour.entity.TourSession;
import com.flourishtravel.domain.tour.repository.TourSessionRepository;
import com.flourishtravel.domain.user.entity.User;
import com.flourishtravel.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final TourSessionRepository sessionRepository;
    private final PromotionRepository promotionRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final BookingGuestRepository bookingGuestRepository;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Transactional(readOnly = true)
    public List<Booking> getMyBookings(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return bookingRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public Booking getById(UUID bookingId, UUID userId) {
        Booking b = bookingRepository.findById(bookingId).orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
        if (!b.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Booking", bookingId);
        }
        return b;
    }

    @Transactional
    public CreateBookingResult create(UUID userId, UUID sessionId, int guestCount, String specialRequests,
                                     String promotionCode, String contactPhone, String pickupAddress, List<String> guestNames,
                                     List<GuestInputDto> guests, String emergencyContactName, String emergencyContactPhone) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        TourSession session = sessionRepository.findById(sessionId).orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));
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
            promotion = promotionRepository.findByCodeAndIsActiveTrue(promotionCode).orElse(null);
            if (promotion != null && Instant.now().isAfter(promotion.getValidFrom()) && Instant.now().isBefore(promotion.getValidTo())) {
                if (promotion.getMinOrderAmount() == null || totalAmount.compareTo(promotion.getMinOrderAmount()) >= 0) {
                    BigDecimal discount = promotion.getDiscountType().equals("percent")
                            ? totalAmount.multiply(promotion.getDiscountValue()).divide(BigDecimal.valueOf(100))
                            : promotion.getDiscountValue();
                    if (promotion.getMaxDiscountAmount() != null && discount.compareTo(promotion.getMaxDiscountAmount()) > 0) {
                        discount = promotion.getMaxDiscountAmount();
                    }
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
        Payment payment = Payment.builder()
                .booking(booking)
                .orderId(orderId)
                .amount(booking.getTotalAmount())
                .status("pending")
                .build();
        paymentRepository.save(payment);

        String paymentUrl = frontendUrl + "/checkout/pay?orderId=" + orderId + "&bookingId=" + booking.getId();
        return new CreateBookingResult(booking, paymentUrl, 15 * 60);
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

    @Transactional(readOnly = true)
    public ValidatePromoResult validatePromo(String code, UUID sessionId, int guestCount) {
        if (code == null || code.isBlank()) {
            return new ValidatePromoResult(false, BigDecimal.ZERO, "Mã không được để trống");
        }
        TourSession session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            return new ValidatePromoResult(false, BigDecimal.ZERO, "Session không tồn tại");
        }
        BigDecimal unitPrice = session.getTour().getBasePrice() != null ? session.getTour().getBasePrice() : BigDecimal.ZERO;
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(guestCount));
        return promotionRepository.findByCodeAndIsActiveTrue(code)
                .filter(p -> Instant.now().isAfter(p.getValidFrom()) && Instant.now().isBefore(p.getValidTo()))
                .filter(p -> p.getUsageLimit() == null || p.getUsedCount() < p.getUsageLimit())
                .filter(p -> p.getMinOrderAmount() == null || totalAmount.compareTo(p.getMinOrderAmount()) >= 0)
                .map(p -> {
                    BigDecimal discount = p.getDiscountType().equals("percent")
                            ? totalAmount.multiply(p.getDiscountValue()).divide(BigDecimal.valueOf(100))
                            : p.getDiscountValue();
                    if (p.getMaxDiscountAmount() != null && discount.compareTo(p.getMaxDiscountAmount()) > 0) {
                        discount = p.getMaxDiscountAmount();
                    }
                    return new ValidatePromoResult(true, discount, "Áp dụng thành công");
                })
                .orElse(new ValidatePromoResult(false, BigDecimal.ZERO, "Mã không hợp lệ hoặc đã hết hạn"));
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

    public record CreateBookingResult(Booking booking, String paymentUrl, int expiresInSeconds) {}
    public record ValidatePromoResult(boolean valid, BigDecimal discountAmount, String message) {}
}
