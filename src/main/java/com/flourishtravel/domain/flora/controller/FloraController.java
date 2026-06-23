package com.flourishtravel.domain.flora.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.domain.flora.dto.FloraJourneyDto;
import com.flourishtravel.domain.flora.dto.FloraLocationRequest;
import com.flourishtravel.domain.flora.dto.FloraLocationResponse;
import com.flourishtravel.domain.flora.dto.TravelPreferencesDto;
import com.flourishtravel.domain.flora.dto.UpdateTravelPreferencesRequest;
import com.flourishtravel.domain.flora.service.FloraJourneyService;
import com.flourishtravel.domain.flora.service.FloraLocationService;
import com.flourishtravel.domain.flora.service.UserTravelPreferenceService;
import com.flourishtravel.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/flora")
@RequiredArgsConstructor
public class FloraController {

    private final FloraJourneyService journeyService;
    private final FloraLocationService locationService;
    private final UserTravelPreferenceService preferenceService;

    @GetMapping("/bookings/{bookingId}/journey")
    public ResponseEntity<ApiResponse<FloraJourneyDto>> journey(
            @PathVariable UUID bookingId,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(ApiResponse.ok(journeyService.getJourney(bookingId, principal.getId())));
    }

    @PostMapping("/bookings/{bookingId}/location")
    public ResponseEntity<ApiResponse<FloraLocationResponse>> location(
            @PathVariable UUID bookingId,
            @RequestBody FloraLocationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(ApiResponse.ok(
                locationService.recordLocation(bookingId, principal.getId(), request)));
    }

    @GetMapping("/preferences/me")
    public ResponseEntity<ApiResponse<TravelPreferencesDto>> getPreferences(
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(ApiResponse.ok(preferenceService.getForUser(principal.getId())));
    }

    @PatchMapping("/preferences/me")
    public ResponseEntity<ApiResponse<TravelPreferencesDto>> updatePreferences(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody UpdateTravelPreferencesRequest request) {
        if (principal == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật sở thích du lịch thành công",
                preferenceService.update(principal.getId(), request)));
    }
}
