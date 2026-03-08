package com.flourishtravel.domain.review.service;

import com.flourishtravel.common.exception.BadRequestException;
import com.flourishtravel.common.exception.ResourceNotFoundException;
import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.booking.repository.BookingRepository;
import com.flourishtravel.domain.review.entity.Review;
import com.flourishtravel.domain.review.repository.ReviewRepository;
import com.flourishtravel.domain.user.entity.User;
import com.flourishtravel.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    @Transactional
    public Review create(UUID userId, UUID bookingId, int rating, String comment) {
        if (rating < 1 || rating > 5) {
            throw new BadRequestException("Rating phải từ 1 đến 5");
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
        if (!booking.getUser().getId().equals(userId)) {
            throw new BadRequestException("Chỉ có thể đánh giá đơn của chính bạn");
        }
        if (!"paid".equals(booking.getStatus()) && !"completed".equals(booking.getStatus())) {
            throw new BadRequestException("Chỉ có thể đánh giá sau khi đã thanh toán và hoàn thành chuyến đi");
        }
        if (reviewRepository.existsByBooking(booking)) {
            throw new BadRequestException("Bạn đã đánh giá đơn này rồi");
        }
        Review review = Review.builder()
                .booking(booking)
                .user(user)
                .tour(booking.getSession().getTour())
                .rating(rating)
                .comment(comment != null ? comment.trim() : null)
                .build();
        return reviewRepository.save(review);
    }
}
