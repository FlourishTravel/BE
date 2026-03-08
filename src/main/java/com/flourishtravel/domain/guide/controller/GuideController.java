package com.flourishtravel.domain.guide.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.domain.booking.entity.SessionCheckin;
import com.flourishtravel.domain.guide.service.GuideService;
import com.flourishtravel.domain.tour.entity.TourSession;
import com.flourishtravel.domain.user.entity.User;
import com.flourishtravel.security.UserPrincipal;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/guide")
@RequiredArgsConstructor
public class GuideController {

    private final GuideService guideService;

    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<List<TourSession>>> getMySessions(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        List<TourSession> sessions = guideService.getMySessions(principal.getId(), year, month, week);
        return ResponseEntity.ok(ApiResponse.ok(sessions));
    }

    @GetMapping("/sessions/{id}")
    public ResponseEntity<ApiResponse<TourSession>> getSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        TourSession session = guideService.getSessionById(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(session));
    }

    @GetMapping("/sessions/{sessionId}/members")
    public ResponseEntity<ApiResponse<List<User>>> getSessionMembers(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        List<User> members = guideService.getSessionMembers(sessionId, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(members));
    }

    @PostMapping("/checkins")
    public ResponseEntity<ApiResponse<SessionCheckin>> checkin(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody GuideCheckinRequest request) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        SessionCheckin checkin = guideService.checkin(
                principal.getId(),
                request.getSessionId(),
                request.getUserId(),
                request.getCheckInType());
        return ResponseEntity.ok(ApiResponse.ok("Đã check-in", checkin));
    }

    @Data
    public static class GuideCheckinRequest {
        private UUID sessionId;
        private UUID userId;
        private String checkInType;
    }
}
