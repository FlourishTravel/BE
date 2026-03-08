package com.flourishtravel.domain.admin.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.common.exception.BadRequestException;
import com.flourishtravel.common.exception.ResourceNotFoundException;
import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.booking.repository.BookingRepository;
import com.flourishtravel.domain.payment.entity.Refund;
import com.flourishtravel.domain.tour.repository.TourSessionRepository;
import com.flourishtravel.domain.payment.repository.RefundRepository;
import com.flourishtravel.domain.user.entity.User;
import com.flourishtravel.domain.user.repository.UserRepository;
import com.flourishtravel.security.UserPrincipal;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminRefundController {

    private final RefundRepository refundRepository;
    private final BookingRepository bookingRepository;
    private final TourSessionRepository sessionRepository;
    private final UserRepository userRepository;

    @PostMapping("/refunds")
    public ResponseEntity<ApiResponse<Refund>> createRefund(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody AdminRefundRequest request) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", request.getBookingId()));
        if (!"paid".equals(booking.getStatus())) {
            throw new BadRequestException("Chỉ có thể hoàn tiền cho đơn đã thanh toán");
        }
        User admin = userRepository.findById(principal.getId()).orElseThrow(() -> new ResourceNotFoundException("User", principal.getId()));
        Refund refund = Refund.builder()
                .booking(booking)
                .amount(request.getAmount() != null ? request.getAmount() : booking.getTotalAmount())
                .reason(request.getReason())
                .status("pending")
                .processedBy(admin)
                .processedAt(Instant.now())
                .build();
        refund = refundRepository.save(refund);
        booking.setStatus("refunded");
        var session = booking.getSession();
        session.setCurrentParticipants(Math.max(0, session.getCurrentParticipants() - booking.getGuestCount()));
        sessionRepository.save(session);
        bookingRepository.save(booking);
        refund.setStatus("completed");
        refundRepository.save(refund);
        return ResponseEntity.ok(ApiResponse.ok("Đã xử lý hoàn tiền", refund));
    }

    @Data
    public static class AdminRefundRequest {
        private UUID bookingId;
        private java.math.BigDecimal amount;
        private String reason;
    }
}
