package com.flourishtravel.domain.flora.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.domain.flora.dto.FloraJourneyDto;
import com.flourishtravel.domain.flora.dto.FloraLocationRequest;
import com.flourishtravel.domain.flora.dto.FloraLocationResponse;
import com.flourishtravel.domain.flora.dto.FloraPostTourFeedbackContextDto;
import com.flourishtravel.domain.flora.dto.FloraPreferencePreviewDto;
import com.flourishtravel.domain.flora.dto.FloraPreferencePreviewRequest;
import com.flourishtravel.domain.flora.dto.TravelPreferencesDto;
import com.flourishtravel.domain.flora.dto.UpdateTravelPreferencesRequest;
import com.flourishtravel.domain.flora.feedback.FloraPostTourFeedbackService;
import com.flourishtravel.domain.flora.service.FloraJourneyService;
import com.flourishtravel.domain.flora.service.FloraLocationService;
import com.flourishtravel.domain.flora.service.UserTravelPreferenceService;
import com.flourishtravel.domain.flora.recommendation.FloraNearbyRecommendationService;
import com.flourishtravel.domain.flora.recommendation.dto.FloraNearbyRecommendationRequest;
import com.flourishtravel.domain.flora.recommendation.dto.FloraNearbyRecommendationResponse;
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
    private final FloraNearbyRecommendationService nearbyRecommendationService;
    private final FloraPostTourFeedbackService postTourFeedbackService;

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

    @PostMapping("/bookings/{bookingId}/nearby-recommendations")
    public ResponseEntity<ApiResponse<FloraNearbyRecommendationResponse>> nearbyRecommendations(
            @PathVariable UUID bookingId,
            @RequestBody(required = false) FloraNearbyRecommendationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        FloraNearbyRecommendationRequest body = request != null ? request : new FloraNearbyRecommendationRequest();
        return ResponseEntity.ok(ApiResponse.ok(
                nearbyRecommendationService.recommend(bookingId, principal.getId(), body)));
    }

    @GetMapping("/bookings/{bookingId}/post-tour-feedback")
    public ResponseEntity<ApiResponse<FloraPostTourFeedbackContextDto>> postTourFeedbackContext(
            @PathVariable UUID bookingId,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(ApiResponse.ok(
                postTourFeedbackService.getContext(bookingId, principal.getId())));
    }

    @PostMapping("/feedback/preference-preview")
    public ResponseEntity<ApiResponse<FloraPreferencePreviewDto>> preferencePreview(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody FloraPreferencePreviewRequest request) {
        if (principal == null) return ResponseEntity.status(401).build();
        FloraPreferencePreviewRequest body = request != null ? request : new FloraPreferencePreviewRequest();
        return ResponseEntity.ok(ApiResponse.ok(
                postTourFeedbackService.previewPreferences(principal.getId(), body.getSelectedTagIds())));
    }
}
