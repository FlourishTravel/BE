package com.flourishtravel.domain.review.service;

import com.flourishtravel.common.exception.BadRequestException;
import com.flourishtravel.common.exception.ResourceNotFoundException;
import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.booking.repository.BookingRepository;
import com.flourishtravel.domain.flora.feedback.FloraPostTourEligibility;
import com.flourishtravel.domain.review.repository.ReviewRepository;
import com.flourishtravel.domain.tour.entity.Tour;
import com.flourishtravel.domain.tour.entity.TourSession;
import com.flourishtravel.domain.user.entity.User;
import com.flourishtravel.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock ReviewRepository reviewRepository;
    @Mock BookingRepository bookingRepository;
    @Mock UserRepository userRepository;
    @Mock FloraPostTourEligibility postTourEligibility;
    @InjectMocks ReviewService reviewService;

    private UUID userId;
    private UUID bookingId;
    private User user;
    private Booking booking;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        bookingId = UUID.randomUUID();
        user = User.builder().build();
        user.setId(userId);
        Tour tour = Tour.builder().build();
        tour.setId(UUID.randomUUID());
        TourSession session = TourSession.builder().tour(tour).build();
        booking = Booking.builder().user(user).session(session).status("completed").build();
        booking.setId(bookingId);
    }

    @Test
    void create_succeedsForEligibleBooking() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(bookingRepository.findDetailForUser(bookingId, userId)).thenReturn(Optional.of(booking));
        when(postTourEligibility.isEligible(booking)).thenReturn(true);
        when(reviewRepository.existsByBooking(booking)).thenReturn(false);
        when(reviewRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var review = reviewService.create(userId, bookingId, 5, "Hay", List.of("COFFEE"));

        assertEquals(5, review.getRating());
        assertEquals("COFFEE", review.getFeedbackTags());
    }

    @Test
    void create_blocksDuplicate() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(bookingRepository.findDetailForUser(bookingId, userId)).thenReturn(Optional.of(booking));
        when(postTourEligibility.isEligible(booking)).thenReturn(true);
        when(reviewRepository.existsByBooking(booking)).thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> reviewService.create(userId, bookingId, 4, null, null));
    }

    @Test
    void create_blocksIneligibleBooking() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(bookingRepository.findDetailForUser(bookingId, userId)).thenReturn(Optional.of(booking));
        when(postTourEligibility.isEligible(booking)).thenReturn(false);

        assertThrows(BadRequestException.class,
                () -> reviewService.create(userId, bookingId, 4, null, null));
    }

    @Test
    void create_blocksAnotherUsersBooking() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(bookingRepository.findDetailForUser(bookingId, userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> reviewService.create(userId, bookingId, 4, null, null));
    }

    @Test
    void create_rejectsUnknownTag() {
        assertThrows(BadRequestException.class,
                () -> reviewService.create(userId, bookingId, 4, null, List.of("UNKNOWN")));
    }

    @Test
    void create_worksWithoutTagsWhenPersonalizationIrrelevant() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(bookingRepository.findDetailForUser(bookingId, userId)).thenReturn(Optional.of(booking));
        when(postTourEligibility.isEligible(booking)).thenReturn(true);
        when(reviewRepository.existsByBooking(booking)).thenReturn(false);
        when(reviewRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var review = reviewService.create(userId, bookingId, 3, "Ổn", null);
        assertNull(review.getFeedbackTags());
    }
}
