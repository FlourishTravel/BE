package com.flourishtravel.domain.review.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.domain.review.dto.ReviewViewDto;
import com.flourishtravel.domain.review.entity.Review;
import com.flourishtravel.domain.review.service.ReviewService;
import com.flourishtravel.security.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ApiResponse<Review>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateReviewRequest request) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        Review review = reviewService.create(
                principal.getId(),
                request.getBookingId(),
                request.getRating(),
                request.getComment(),
                request.getFeedbackTags());
        return ResponseEntity.ok(ApiResponse.ok("Đã gửi đánh giá", review));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<ReviewViewDto>>> listMine(
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(ApiResponse.ok(reviewService.listMine(principal.getId())));
    }

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<ReviewViewDto>>> listPublic(
            @RequestParam(required = false) UUID tourId) {
        return ResponseEntity.ok(ApiResponse.ok(reviewService.listPublic(tourId)));
    }

    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<List<ReviewViewDto>>> listFeatured() {
        return ResponseEntity.ok(ApiResponse.ok(reviewService.listPublicFeatured()));
    }

    @Data
    public static class CreateReviewRequest {
        private UUID bookingId;
        @Min(1)
        @Max(5)
        private int rating = 5;
        @Size(max = 2000)
        private String comment;
        @Size(max = 10)
        private List<String> feedbackTags;
    }
}
