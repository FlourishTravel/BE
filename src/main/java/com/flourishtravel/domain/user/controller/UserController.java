package com.flourishtravel.domain.user.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.domain.user.dto.UpdateProfileRequest;
import com.flourishtravel.domain.user.dto.UserProfileResponse;
import com.flourishtravel.domain.user.service.UserService;
import com.flourishtravel.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMe(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        UserProfileResponse profile = userService.getProfile(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMe(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody UpdateProfileRequest request) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        UserProfileResponse profile = userService.updateProfile(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật hồ sơ thành công", profile));
    }

    @GetMapping("/me/travel-preferences")
    public ResponseEntity<ApiResponse<com.flourishtravel.domain.flora.dto.TravelPreferencesDto>> getTravelPreferences(
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(ApiResponse.ok(
                userService.getTravelPreferences(principal.getId())));
    }

    @PatchMapping("/me/travel-preferences")
    public ResponseEntity<ApiResponse<com.flourishtravel.domain.flora.dto.TravelPreferencesDto>> updateTravelPreferences(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody com.flourishtravel.domain.flora.dto.UpdateTravelPreferencesRequest request) {
        if (principal == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật sở thích du lịch thành công",
                userService.updateTravelPreferences(principal.getId(), request)));
    }
}
