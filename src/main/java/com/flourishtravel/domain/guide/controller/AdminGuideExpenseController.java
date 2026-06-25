package com.flourishtravel.domain.guide.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.domain.guide.dto.GuideSessionExpenseDto;
import com.flourishtravel.domain.guide.dto.UpdateGuideExpenseStatusRequest;
import com.flourishtravel.domain.guide.service.GuideSessionExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/guide-expenses")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminGuideExpenseController {

    private final GuideSessionExpenseService expenseService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<GuideSessionExpenseDto>>> list(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.ok(expenseService.listForAdmin(status)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<GuideSessionExpenseDto>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateGuideExpenseStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Đã cập nhật trạng thái", expenseService.updateStatus(id, request)));
    }
}
