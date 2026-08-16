package com.flourishtravel.domain.admin.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.domain.admin.dto.AdminWaitlistEntryDto;
import com.flourishtravel.domain.admin.service.AdminTourRosterService;
import com.flourishtravel.domain.guide.dto.GuideSessionGuestsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminTourRosterController {

    private final AdminTourRosterService rosterService;

    /** Danh sách khách + lịch sử check-in/check-out HDV của một đợt khởi hành. */
    @GetMapping("/sessions/{sessionId}/guests")
    public ResponseEntity<ApiResponse<GuideSessionGuestsDto>> sessionGuests(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(ApiResponse.ok(rosterService.sessionGuests(sessionId)));
    }

    /** Danh sách chờ chỗ / chờ lịch mới của tour. */
    @GetMapping("/tours/{tourId}/waitlist")
    public ResponseEntity<ApiResponse<List<AdminWaitlistEntryDto>>> tourWaitlist(@PathVariable UUID tourId) {
        return ResponseEntity.ok(ApiResponse.ok(rosterService.waitlistForTour(tourId)));
    }
}
