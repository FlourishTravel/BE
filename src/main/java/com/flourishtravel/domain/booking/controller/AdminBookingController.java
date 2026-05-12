package com.flourishtravel.domain.booking.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.domain.booking.dto.AdminBookingDetailDto;
import com.flourishtravel.domain.booking.dto.AdminBookingSummaryDto;
import com.flourishtravel.domain.booking.dto.BookingStatsDto;
import com.flourishtravel.domain.booking.dto.BookingStatusUpdateRequest;
import com.flourishtravel.domain.booking.dto.RefundActionRequest;
import com.flourishtravel.domain.booking.service.AdminBookingService;
import com.flourishtravel.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.Instant;
import java.util.UUID;

/**
 * REST endpoints quản lý booking dành cho admin.
 *
 * Tất cả endpoints yêu cầu role ADMIN.
 */
@RestController
@RequestMapping("/bookings/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminBookingController {

    private static final ZoneId ZONE_VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private final AdminBookingService adminBookingService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminBookingSummaryDto>>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Instant fromInstant = from == null ? null : from.atStartOfDay(ZONE_VN).toInstant();
        Instant toInstant = to == null ? null : to.plusDays(1).atStartOfDay(ZONE_VN).toInstant();
        Page<AdminBookingSummaryDto> result = adminBookingService.adminList(
                q, status, fromInstant, toInstant, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<BookingStatsDto>> stats() {
        return ResponseEntity.ok(ApiResponse.ok(adminBookingService.stats()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminBookingDetailDto>> detail(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(adminBookingService.adminDetail(id)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<AdminBookingDetailDto>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody BookingStatusUpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Đã cập nhật trạng thái booking",
                adminBookingService.updateStatus(id, req)));
    }

    @PostMapping("/{id}/mark-paid")
    public ResponseEntity<ApiResponse<AdminBookingDetailDto>> markPaid(
            @PathVariable UUID id,
            @RequestBody(required = false) MarkPaidRequest req) {
        BigDecimal amount = req == null ? null : req.getAmount();
        String note = req == null ? null : req.getNote();
        return ResponseEntity.ok(ApiResponse.ok(
                "Đã ghi nhận thanh toán",
                adminBookingService.markPaid(id, amount, note)));
    }

    @PostMapping("/{id}/refund/approve")
    public ResponseEntity<ApiResponse<AdminBookingDetailDto>> approveRefund(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestBody(required = false) RefundActionRequest req) {
        UUID adminId = principal == null ? null : principal.getId();
        return ResponseEntity.ok(ApiResponse.ok(
                "Đã duyệt hoàn tiền",
                adminBookingService.approveRefund(id, adminId, req)));
    }

    @PostMapping("/{id}/refund/reject")
    public ResponseEntity<ApiResponse<AdminBookingDetailDto>> rejectRefund(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody RefundActionRequest req) {
        UUID adminId = principal == null ? null : principal.getId();
        return ResponseEntity.ok(ApiResponse.ok(
                "Đã từ chối hoàn tiền",
                adminBookingService.rejectRefund(id, adminId, req)));
    }

    @Data
    public static class MarkPaidRequest {
        private BigDecimal amount;
        private String note;
    }
}
