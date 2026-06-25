package com.flourishtravel.domain.review.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.domain.review.dto.ReviewModerationRequest;
import com.flourishtravel.domain.review.dto.ReviewViewDto;
import com.flourishtravel.domain.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/reviews")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ReviewViewDto>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(reviewService.listAdmin()));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<ReviewViewDto>> updateModeration(
            @PathVariable UUID id,
            @RequestBody ReviewModerationRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Đã cập nhật kiểm duyệt", reviewService.updateModeration(id, request)));
    }
}
