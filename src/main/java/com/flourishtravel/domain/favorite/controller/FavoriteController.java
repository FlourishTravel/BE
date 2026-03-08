package com.flourishtravel.domain.favorite.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.domain.favorite.service.FavoriteService;
import com.flourishtravel.domain.tour.entity.Tour;
import com.flourishtravel.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Tour>>> getMyFavorites(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        List<Tour> tours = favoriteService.getMyFavorites(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(tours));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> add(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody java.util.Map<String, UUID> body) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        UUID tourId = body.get("tour_id");
        if (tourId == null) {
            tourId = body.get("tourId");
        }
        if (tourId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("tour_id là bắt buộc"));
        }
        favoriteService.add(principal.getId(), tourId);
        return ResponseEntity.ok(ApiResponse.ok("Đã thêm vào yêu thích", null));
    }

    @DeleteMapping("/{tourId}")
    public ResponseEntity<ApiResponse<Void>> remove(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID tourId) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        favoriteService.remove(principal.getId(), tourId);
        return ResponseEntity.ok(ApiResponse.ok("Đã bỏ khỏi yêu thích", null));
    }
}
