package com.flourishtravel.domain.booking.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.common.exception.BadRequestException;
import com.flourishtravel.common.exception.ResourceNotFoundException;
import com.flourishtravel.domain.booking.entity.SessionWaitlist;
import com.flourishtravel.domain.booking.repository.SessionWaitlistRepository;
import com.flourishtravel.domain.tour.entity.Tour;
import com.flourishtravel.domain.tour.entity.TourSession;
import com.flourishtravel.domain.tour.repository.TourRepository;
import com.flourishtravel.domain.tour.repository.TourSessionRepository;
import com.flourishtravel.domain.user.repository.UserRepository;
import com.flourishtravel.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/waitlist")
@RequiredArgsConstructor
public class WaitlistController {

    private final SessionWaitlistRepository waitlistRepository;
    private final UserRepository userRepository;
    private final TourRepository tourRepository;
    private final TourSessionRepository sessionRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<SessionWaitlist>> add(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody WaitlistRequest dto) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        var user = userRepository.findById(principal.getId()).orElseThrow(() -> new ResourceNotFoundException("User", principal.getId()));
        if (dto.getSessionId() != null) {
            TourSession session = sessionRepository.findById(dto.getSessionId()).orElseThrow(() -> new ResourceNotFoundException("Session", dto.getSessionId()));
            if (waitlistRepository.existsByUserAndSession(user, session)) {
                return ResponseEntity.ok(ApiResponse.ok("Bạn đã đăng ký nhận thông báo cho lịch này", null));
            }
            SessionWaitlist w = waitlistRepository.save(SessionWaitlist.builder().user(user).session(session).status("waiting").build());
            return ResponseEntity.ok(ApiResponse.ok("Đã đăng ký nhận thông báo khi có chỗ trống", w));
        }
        if (dto.getTourId() != null) {
            Tour tour = tourRepository.findById(dto.getTourId()).orElseThrow(() -> new ResourceNotFoundException("Tour", dto.getTourId()));
            if (waitlistRepository.existsByUserAndTourAndSessionIsNull(user, tour)) {
                return ResponseEntity.ok(ApiResponse.ok("Bạn đã đăng ký nhận thông báo cho tour này", null));
            }
            SessionWaitlist w = waitlistRepository.save(SessionWaitlist.builder().user(user).tour(tour).status("waiting").build());
            return ResponseEntity.ok(ApiResponse.ok("Đã đăng ký nhận thông báo khi có lịch mới", w));
        }
        throw new BadRequestException("Cần truyền session_id hoặc tour_id");
    }

    @lombok.Data
    public static class WaitlistRequest {
        private UUID tourId;
        private UUID sessionId;
    }
}
