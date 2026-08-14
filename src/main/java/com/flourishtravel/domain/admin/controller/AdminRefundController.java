package com.flourishtravel.domain.admin.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.common.exception.BadRequestException;
import com.flourishtravel.common.exception.ResourceNotFoundException;
import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.booking.repository.BookingRepository;
import com.flourishtravel.domain.payment.entity.Refund;
import com.flourishtravel.domain.payment.repository.RefundRepository;
import com.flourishtravel.domain.payment.service.RefundReasonRules;
import com.flourishtravel.domain.user.entity.User;
import com.flourishtravel.domain.user.repository.UserRepository;
import com.flourishtravel.security.UserPrincipal;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Tạo yêu cầu hoàn tiền (pending). Admin duyệt ở booking để PayOS chi hộ tự động.
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminRefundController {

    private final RefundRepository refundRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    @PostMapping("/refunds")
    public ResponseEntity<ApiResponse<Refund>> createRefund(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody AdminRefundRequest request) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        if (request == null || request.getBookingId() == null) {
            throw new BadRequestException("Thiếu mã booking");
        }
        String reason = RefundReasonRules.requireValid(request.getReason(), "hoàn tiền");
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", request.getBookingId()));
        if (!"paid".equals(booking.getStatus()) && !"confirmed".equals(booking.getStatus())) {
            throw new BadRequestException("Chỉ có thể tạo hoàn tiền cho đơn đã thanh toán");
        }
        boolean hasPending = booking.getRefunds() != null && booking.getRefunds().stream()
                .anyMatch(r -> "pending".equalsIgnoreCase(r.getStatus()));
        if (hasPending) {
            throw new BadRequestException("Đã có yêu cầu hoàn tiền đang chờ duyệt");
        }
        User admin = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.getId()));
        Refund refund = Refund.builder()
                .booking(booking)
                .amount(request.getAmount() != null ? request.getAmount() : booking.getTotalAmount())
                .reason(reason)
                .status("pending")
                .processedBy(admin)
                .build();
        refund = refundRepository.save(refund);
        return ResponseEntity.ok(ApiResponse.ok(
                "Đã tạo yêu cầu hoàn tiền. Duyệt ở chi tiết booking để PayOS chi hộ tự động.",
                refund));
    }

    @Data
    public static class AdminRefundRequest {
        private UUID bookingId;
        private java.math.BigDecimal amount;
        private String reason;
    }
}
