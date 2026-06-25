package com.flourishtravel.domain.notification.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.domain.notification.dto.AdminNotificationSummaryDto;
import com.flourishtravel.domain.notification.dto.BroadcastNotificationRequest;
import com.flourishtravel.domain.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Locale;

@RestController
@RequestMapping("/admin/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminNotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminNotificationSummaryDto>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        return ResponseEntity.ok(ApiResponse.ok(notificationService.listForAdmin(page, size)));
    }

    @PostMapping("/broadcast")
    public ResponseEntity<ApiResponse<Map<String, Object>>> broadcast(@Valid @RequestBody BroadcastNotificationRequest request) {
        String role = request.getTargetRole() == null || request.getTargetRole().isBlank()
                ? "TRAVELER"
                : request.getTargetRole().trim().toUpperCase(Locale.ROOT);
        int sent = "ALL".equals(role)
                ? notificationService.broadcastToAll(request.getTitle().trim(), request.getBody().trim(), request.getType())
                : notificationService.broadcastToRole(role, request.getTitle().trim(), request.getBody().trim(), request.getType());
        return ResponseEntity.ok(ApiResponse.ok("Đã gửi thông báo",
                Map.of("sentCount", sent, "targetRole", role)));
    }
}
