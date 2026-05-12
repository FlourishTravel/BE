package com.flourishtravel.domain.user.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.domain.user.dto.AdminStaffDetailDto;
import com.flourishtravel.domain.user.dto.AdminStaffSummaryDto;
import com.flourishtravel.domain.user.dto.CreateStaffRequest;
import com.flourishtravel.domain.user.dto.ResetStaffPasswordRequest;
import com.flourishtravel.domain.user.dto.StaffStatsDto;
import com.flourishtravel.domain.user.dto.UpdateStaffRequest;
import com.flourishtravel.domain.user.service.AdminStaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * API quản lý nhân sự nội bộ (ADMIN / TOUR_GUIDE / STAFF).
 *
 * Base path: /users/admin/staff/**
 */
@RestController
@RequestMapping("/users/admin/staff")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminStaffController {

    private final AdminStaffService adminStaffService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<StaffStatsDto>> stats() {
        return ResponseEntity.ok(ApiResponse.ok(adminStaffService.stats()));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminStaffSummaryDto>>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String employmentStatus,
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) String department,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100));
        Page<AdminStaffSummaryDto> result = adminStaffService.list(q, employmentStatus, roleName, department, pageable);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminStaffDetailDto>> detail(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(adminStaffService.detail(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AdminStaffDetailDto>> create(@Valid @RequestBody CreateStaffRequest req) {
        AdminStaffDetailDto dto = adminStaffService.create(req);
        return ResponseEntity.ok(ApiResponse.ok("Đã tạo nhân viên", dto));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminStaffDetailDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStaffRequest req) {
        AdminStaffDetailDto dto = adminStaffService.update(id, req);
        return ResponseEntity.ok(ApiResponse.ok("Đã cập nhật nhân viên", dto));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<AdminStaffDetailDto>> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Đã kích hoạt", adminStaffService.setActive(id, true)));
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<AdminStaffDetailDto>> deactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Đã vô hiệu hoá", adminStaffService.setActive(id, false)));
    }

    @PatchMapping("/{id}/password")
    public ResponseEntity<ApiResponse<AdminStaffDetailDto>> resetPassword(
            @PathVariable UUID id,
            @Valid @RequestBody ResetStaffPasswordRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Đã đặt lại mật khẩu", adminStaffService.resetPassword(id, req)));
    }
}
