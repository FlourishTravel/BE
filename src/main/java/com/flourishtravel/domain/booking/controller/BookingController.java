package com.flourishtravel.domain.booking.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.domain.booking.dto.CreateBookingResponse;
import com.flourishtravel.domain.booking.dto.GuestInputDto;
import com.flourishtravel.domain.booking.dto.MomoPayUrlResponse;
import com.flourishtravel.domain.booking.dto.MomoSyncFromReturnRequest;
import com.flourishtravel.domain.booking.dto.UserBookingDetailDto;
import com.flourishtravel.domain.booking.dto.UserBookingSummaryDto;
import com.flourishtravel.domain.booking.dto.ValidateSessionRequest;
import com.flourishtravel.domain.booking.service.BookingService;
import com.flourishtravel.domain.payment.entity.Refund;
import com.flourishtravel.security.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<UserBookingSummaryDto>>> getMyBookings(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        List<UserBookingSummaryDto> list = bookingService.listMyBookingSummaries(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PostMapping("/validate-session")
    public ResponseEntity<ApiResponse<BookingService.ValidateSessionResult>> validateSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ValidateSessionRequest request) {
        UUID userId = principal != null ? principal.getId() : null;
        BookingService.ValidateSessionResult result = bookingService.validateSessionForBooking(
                request.getTourId(),
                request.getSessionId(),
                request.getGuestCount(),
                userId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserBookingDetailDto>> getMyBookingDetail(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        UserBookingDetailDto dto = bookingService.getMyBookingDetail(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CreateBookingResponse>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateBookingRequest request) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        List<GuestInputDto> guestDtos = null;
        if (request.getGuests() != null && !request.getGuests().isEmpty()) {
            guestDtos = request.getGuests().stream()
                    .map(g -> {
                        GuestInputDto d = new GuestInputDto();
                        d.setFullName(g.getFullName());
                        d.setIdNumber(g.getIdNumber());
                        d.setDateOfBirth(g.getDateOfBirth());
                        return d;
                    })
                    .toList();
        }
        CreateBookingResponse result = bookingService.create(
                principal.getId(),
                request.getSessionId(),
                request.getGuestCount(),
                request.getSpecialRequests(),
                request.getPromotionCode(),
                request.getContactPhone(),
                request.getPickupAddress(),
                request.getGuestNames(),
                guestDtos,
                request.getEmergencyContactName(),
                request.getEmergencyContactPhone(),
                request.getPaymentMethod());
        return ResponseEntity.ok(ApiResponse.ok("Tạo đơn thành công", result));
    }

    @PostMapping("/{id}/momo-pay-url")
    public ResponseEntity<ApiResponse<MomoPayUrlResponse>> momoPayUrl(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        MomoPayUrlResponse body = bookingService.resumeMomoPaymentUrl(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(body));
    }

    @PostMapping("/momo/sync-from-return")
    public ResponseEntity<ApiResponse<Void>> syncMomoFromReturn(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody MomoSyncFromReturnRequest body) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        bookingService.syncMomoPaymentAfterReturn(principal.getId(), body.getOrderId());
        return ResponseEntity.ok(ApiResponse.ok("Đã cập nhật trạng thái thanh toán", null));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancel(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        bookingService.cancel(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Đã hủy đơn", null));
    }

    @PostMapping("/validate-promo")
    public ResponseEntity<ApiResponse<BookingService.ValidatePromoResult>> validatePromo(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody ValidatePromoRequest request) {
        UUID userId = principal != null ? principal.getId() : null;
        BookingService.ValidatePromoResult result = bookingService.validatePromo(
                request.getCode(),
                request.getSessionId(),
                request.getGuestCount() != null ? request.getGuestCount() : 1,
                userId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/{id}/request-refund")
    public ResponseEntity<ApiResponse<Refund>> requestRefund(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestBody(required = false) RequestRefundBody body) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        String reason = body != null ? body.getReason() : null;
        Refund refund = bookingService.requestRefund(id, principal.getId(), reason);
        return ResponseEntity.ok(ApiResponse.ok("Đã gửi yêu cầu hoàn tiền", refund));
    }

    @Data
    public static class CreateBookingRequest {
        private UUID sessionId;
        @Min(1)
        private int guestCount = 1;
        private String specialRequests;
        private String promotionCode;
        /** Số điện thoại liên hệ chuyến đi (nếu trống lấy từ tài khoản). */
        private String contactPhone;
        /** Điểm đón (tour xe bus). */
        private String pickupAddress;
        /** Danh sách tên từng khách (tương thích cũ). */
        private List<String> guestNames;
        /** Chi tiết từng khách: tên, CCCD/CMND, ngày sinh (bảo hiểm, vé, hạn chế tuổi). */
        private List<GuestItem> guests;
        /** Liên hệ khẩn cấp – tên người thân. */
        private String emergencyContactName;
        /** Liên hệ khẩn cấp – SĐT người thân. */
        private String emergencyContactPhone;
        /** ewallet | bank | card — ewallet dùng MoMo (sandbox/prod theo cấu hình). */
        private String paymentMethod;
    }

    @Data
    public static class GuestItem {
        private String fullName;
        /** CCCD/CMND. */
        private String idNumber;
        /** Ngày sinh (yyyy-MM-dd). */
        private LocalDate dateOfBirth;
    }

    @Data
    public static class ValidatePromoRequest {
        private String code;
        private UUID sessionId;
        private Integer guestCount;
    }

    @Data
    public static class RequestRefundBody {
        private String reason;
    }
}
