package com.flourishtravel.domain.review.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.domain.review.entity.Review;
import com.flourishtravel.domain.review.service.ReviewService;
import com.flourishtravel.security.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
                request.getComment());
        return ResponseEntity.ok(ApiResponse.ok("Đã gửi đánh giá", review));
    }

    @Data
    public static class CreateReviewRequest {
        private UUID bookingId;
        @Min(1)
        @Max(5)
        private int rating = 5;
        private String comment;
    }
}
