package com.flourishtravel.domain.notification.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.domain.notification.entity.Notification;
import com.flourishtravel.domain.notification.service.NotificationService;
import com.flourishtravel.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<Notification>>> getMy(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Boolean unread_only,
            @RequestParam(required = false) Integer limit) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        Page<Notification> page = notificationService.getMyNotifications(
                principal.getId(), unread_only, limit);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Notification>> markRead(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable java.util.UUID id) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        Notification n = notificationService.markAsRead(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Đã đánh dấu đọc", n));
    }

    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllRead(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        notificationService.markAllAsRead(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Đã đánh dấu tất cả đã đọc", null));
    }
}
