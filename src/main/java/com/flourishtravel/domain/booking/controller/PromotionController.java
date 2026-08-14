package com.flourishtravel.domain.booking.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.domain.booking.dto.PromotionDto;
import com.flourishtravel.domain.booking.service.PromotionAdminService;
import com.flourishtravel.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionAdminService promotionAdminService;

    /**
     * Mã đang hiệu lực cho trang Voucher.
     * Khách vãng lai chỉ thấy mã công khai; đã đăng nhập thì thêm mã được tặng riêng.
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<PromotionDto>>> listActive(
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID userId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(ApiResponse.ok(promotionAdminService.listVisibleTo(userId)));
    }
}
