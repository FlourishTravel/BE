package com.flourishtravel.domain.booking.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.domain.booking.dto.CreatePromotionRequest;
import com.flourishtravel.domain.booking.dto.PromotionDto;
import com.flourishtravel.domain.booking.dto.UpdatePromotionRequest;
import com.flourishtravel.domain.booking.service.PromotionAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/promotions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPromotionController {

    private final PromotionAdminService promotionAdminService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PromotionDto>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(promotionAdminService.list()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PromotionDto>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(promotionAdminService.get(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PromotionDto>> create(@Valid @RequestBody CreatePromotionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Đã tạo khuyến mãi", promotionAdminService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PromotionDto>> update(
            @PathVariable UUID id,
            @RequestBody UpdatePromotionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Đã cập nhật khuyến mãi", promotionAdminService.update(id, request)));
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<PromotionDto>> deactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Đã ngưng khuyến mãi", promotionAdminService.deactivate(id)));
    }
}
