package com.flourishtravel.domain.review.service;

import com.flourishtravel.common.exception.BadRequestException;
import com.flourishtravel.common.exception.ResourceNotFoundException;
import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.booking.repository.BookingRepository;
import com.flourishtravel.domain.flora.feedback.FloraFeedbackTagCatalog;
import com.flourishtravel.domain.flora.feedback.FloraGuideFeedbackTagCatalog;
import com.flourishtravel.domain.flora.feedback.FloraPostTourEligibility;
import com.flourishtravel.domain.review.dto.ReviewModerationRequest;
import com.flourishtravel.domain.review.dto.ReviewViewDto;
import com.flourishtravel.domain.review.entity.Review;
import com.flourishtravel.domain.review.repository.ReviewRepository;
import com.flourishtravel.domain.user.entity.User;
import com.flourishtravel.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private static final int MAX_COMMENT_LENGTH = 2000;

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final FloraPostTourEligibility postTourEligibility;

    @Transactional
    public Review create(UUID userId, UUID bookingId, int rating, String comment) {
        return create(userId, bookingId, rating, comment, null);
    }

    @Transactional
    public Review create(UUID userId, UUID bookingId, int rating, String comment, List<String> feedbackTags) {
        return create(userId, bookingId, rating, comment, feedbackTags, null, null);
    }

    @Transactional
    public Review create(
            UUID userId,
            UUID bookingId,
            int rating,
            String comment,
            List<String> feedbackTags,
            Integer guideRating,
            List<String> guideFeedbackTags) {
        if (rating < 1 || rating > 5) {
            throw new BadRequestException("Rating phải từ 1 đến 5");
        }
        if (comment != null && comment.length() > MAX_COMMENT_LENGTH) {
            throw new BadRequestException("Bình luận tối đa " + MAX_COMMENT_LENGTH + " ký tự");
        }
        try {
            FloraFeedbackTagCatalog.validateTagIds(feedbackTags);
            FloraGuideFeedbackTagCatalog.validateTagIds(guideFeedbackTags);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ex.getMessage());
        }
        if (guideRating != null && (guideRating < 1 || guideRating > 5)) {
            throw new BadRequestException("Điểm HDV phải từ 1 đến 5");
        }

        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Booking booking = bookingRepository.findDetailForUser(bookingId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
        if (!booking.getUser().getId().equals(userId)) {
            throw new BadRequestException("Chỉ có thể đánh giá đơn của chính bạn");
        }
        if (!postTourEligibility.isEligible(booking)) {
            throw new BadRequestException("Chỉ có thể đánh giá sau khi chuyến đi đã kết thúc");
        }
        if (reviewRepository.existsByBooking(booking)) {
            throw new BadRequestException("Bạn đã đánh giá đơn này rồi");
        }

        User guide = booking.getSession() != null ? booking.getSession().getTourGuide() : null;
        if (guide == null && (guideRating != null || (guideFeedbackTags != null && !guideFeedbackTags.isEmpty()))) {
            throw new BadRequestException("Chuyến này chưa có HDV để đánh giá");
        }

        Review review = Review.builder()
                .booking(booking)
                .user(user)
                .tour(booking.getSession().getTour())
                .rating(rating)
                .comment(comment != null ? comment.trim() : null)
                .feedbackTags(FloraFeedbackTagCatalog.joinTagIds(feedbackTags))
                .guideId(guide != null ? guide.getId() : null)
                .guideName(guide != null ? guide.getFullName() : null)
                .guideRating(guide != null ? guideRating : null)
                .guideFeedbackTags(guide != null ? FloraFeedbackTagCatalog.joinTagIds(guideFeedbackTags) : null)
                .build();
        return reviewRepository.save(review);
    }

    @Transactional(readOnly = true)
    public List<ReviewViewDto> listAdmin() {
        return reviewRepository.findAllAdmin().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public ReviewViewDto updateModeration(UUID reviewId, ReviewModerationRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId));
        if (request.getIsPublished() != null) {
            review.setIsPublished(request.getIsPublished());
            if (!request.getIsPublished()) {
                review.setIsFeatured(false);
            }
        }
        if (request.getIsFeatured() != null) {
            review.setIsFeatured(Boolean.TRUE.equals(request.getIsFeatured()) && Boolean.TRUE.equals(review.getIsPublished()));
        }
        return toDto(reviewRepository.save(review));
    }

    @Transactional(readOnly = true)
    public List<ReviewViewDto> listPublic(UUID tourId) {
        return reviewRepository.findPublic(tourId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReviewViewDto> listPublicFeatured() {
        return reviewRepository.findFeaturedPublic().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReviewViewDto> listMine(UUID userId) {
        return reviewRepository.findByUserId(userId).stream()
                .map(this::toDto)
                .toList();
    }

    private ReviewViewDto toDto(Review review) {
        return ReviewViewDto.builder()
                .id(review.getId())
                .bookingId(review.getBooking() != null ? review.getBooking().getId() : null)
                .userId(review.getUser() != null ? review.getUser().getId() : null)
                .userName(review.getUser() != null ? review.getUser().getFullName() : null)
                .tourId(review.getTour() != null ? review.getTour().getId() : null)
                .tourTitle(review.getTour() != null ? review.getTour().getTitle() : null)
                .rating(review.getRating())
                .comment(review.getComment())
                .feedbackTags(review.getFeedbackTags())
                .guideId(review.getGuideId())
                .guideName(review.getGuideName())
                .guideRating(review.getGuideRating())
                .guideFeedbackTags(review.getGuideFeedbackTags())
                .isPublished(review.getIsPublished())
                .isFeatured(review.getIsFeatured())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
