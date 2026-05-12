package com.flourishtravel.domain.user.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.domain.user.dto.AdminCustomerDetailDto;
import com.flourishtravel.domain.user.dto.AdminCustomerSummaryDto;
import com.flourishtravel.domain.user.dto.AdminUpdateCustomerRequest;
import com.flourishtravel.domain.user.dto.CustomerStatsDto;
import com.flourishtravel.domain.user.service.AdminCustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST endpoints chỉ-admin cho trang Quản Lý Khách Hàng.
 *
 * Quyền truy cập: ADMIN.
 *
 * Đường dẫn: /users/admin/**
 */
@RestController
@RequestMapping("/users/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCustomerController {

    private final AdminCustomerService adminCustomerService;

    @GetMapping("/customers")
    public ResponseEntity<ApiResponse<Page<AdminCustomerSummaryDto>>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String tier,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100));
        Page<AdminCustomerSummaryDto> result = adminCustomerService.adminList(q, tier, active, pageable);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<CustomerStatsDto>> stats() {
        return ResponseEntity.ok(ApiResponse.ok(adminCustomerService.stats()));
    }

    @GetMapping("/customers/{id}")
    public ResponseEntity<ApiResponse<AdminCustomerDetailDto>> detail(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(adminCustomerService.adminDetail(id)));
    }

    @PatchMapping("/customers/{id}")
    public ResponseEntity<ApiResponse<AdminCustomerDetailDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody AdminUpdateCustomerRequest req) {
        AdminCustomerDetailDto detail = adminCustomerService.updateCustomer(id, req);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật khách hàng thành công", detail));
    }

    @PostMapping("/customers/{id}/activate")
    public ResponseEntity<ApiResponse<AdminCustomerDetailDto>> activate(@PathVariable UUID id) {
        AdminCustomerDetailDto detail = adminCustomerService.setActive(id, true);
        return ResponseEntity.ok(ApiResponse.ok("Đã kích hoạt khách hàng", detail));
    }

    @PostMapping("/customers/{id}/deactivate")
    public ResponseEntity<ApiResponse<AdminCustomerDetailDto>> deactivate(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body) {
        AdminCustomerDetailDto detail = adminCustomerService.setActive(id, false);
        return ResponseEntity.ok(ApiResponse.ok("Đã vô hiệu hoá khách hàng", detail));
    }
}
