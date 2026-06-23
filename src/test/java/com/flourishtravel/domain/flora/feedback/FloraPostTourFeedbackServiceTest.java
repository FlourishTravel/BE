package com.flourishtravel.domain.flora.feedback;

import com.flourishtravel.common.exception.BadRequestException;
import com.flourishtravel.common.exception.ResourceNotFoundException;
import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.flora.dto.TravelPreferencesDto;
import com.flourishtravel.domain.flora.service.FloraPrivacyService;
import com.flourishtravel.domain.flora.service.UserTravelPreferenceService;
import com.flourishtravel.domain.review.entity.Review;
import com.flourishtravel.domain.review.repository.ReviewRepository;
import com.flourishtravel.domain.tour.entity.Tour;
import com.flourishtravel.domain.tour.entity.TourSession;
import com.flourishtravel.domain.user.entity.User;
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
class FloraPostTourFeedbackServiceTest {

    @Mock FloraPrivacyService privacyService;
    @Mock FloraPostTourEligibility eligibility;
    @Mock ReviewRepository reviewRepository;
    @Mock UserTravelPreferenceService preferenceService;
    @InjectMocks FloraPostTourFeedbackService service;

    private UUID bookingId;
    private UUID userId;
    private Booking booking;

    @BeforeEach
    void setUp() {
        bookingId = UUID.randomUUID();
        userId = UUID.randomUUID();
        Tour tour = Tour.builder().title("Đà Lạt 3 ngày 2 đêm").build();
        TourSession session = TourSession.builder().tour(tour).build();
        booking = Booking.builder().user(User.builder().build()).session(session).build();
        booking.setId(bookingId);
    }

    @Test
    void getContext_returnsTagsWhenPersonalizationEnabled() {
        when(privacyService.requireOwnedBooking(bookingId, userId)).thenReturn(booking);
        when(eligibility.isEligible(booking)).thenReturn(true);
        when(reviewRepository.findByBooking(booking)).thenReturn(Optional.empty());
        when(privacyService.hasPersonalizationConsent(userId)).thenReturn(true);

        var ctx = service.getContext(bookingId, userId);

        assertTrue(ctx.isEligible());
        assertFalse(ctx.isAlreadySubmitted());
        assertTrue(ctx.isPersonalizationEnabled());
        assertFalse(ctx.getAvailableTags().isEmpty());
    }

    @Test
    void getContext_hidesTagsWhenPersonalizationDisabled() {
        when(privacyService.requireOwnedBooking(bookingId, userId)).thenReturn(booking);
        when(eligibility.isEligible(booking)).thenReturn(true);
        when(reviewRepository.findByBooking(booking)).thenReturn(Optional.empty());
        when(privacyService.hasPersonalizationConsent(userId)).thenReturn(false);

        var ctx = service.getContext(bookingId, userId);

        assertTrue(ctx.getAvailableTags().isEmpty());
        assertFalse(ctx.isPersonalizationEnabled());
    }

    @Test
    void getContext_showsExistingReview() {
        Review review = Review.builder().rating(5).comment("Tuyệt").feedbackTags("PHOTO_SPOTS").build();
        when(privacyService.requireOwnedBooking(bookingId, userId)).thenReturn(booking);
        when(eligibility.isEligible(booking)).thenReturn(true);
        when(reviewRepository.findByBooking(booking)).thenReturn(Optional.of(review));
        when(privacyService.hasPersonalizationConsent(userId)).thenReturn(true);

        var ctx = service.getContext(bookingId, userId);

        assertTrue(ctx.isAlreadySubmitted());
        assertEquals(5, ctx.getExistingFeedback().getRating());
        assertEquals(List.of("PHOTO_SPOTS"), ctx.getExistingFeedback().getFeedbackTags());
    }

    @Test
    void previewPreferences_emptyWhenPersonalizationOff() {
        when(privacyService.hasPersonalizationConsent(userId)).thenReturn(false);
        when(preferenceService.getForUser(userId)).thenReturn(TravelPreferencesDto.builder().build());

        var preview = service.previewPreferences(userId, List.of("COFFEE"));

        assertTrue(preview.getChanges().isEmpty());
        verify(preferenceService, never()).update(any(), any());
    }

    @Test
    void cannotAccessAnotherUsersBooking() {
        when(privacyService.requireOwnedBooking(bookingId, userId))
                .thenThrow(new ResourceNotFoundException("Booking", bookingId));

        assertThrows(ResourceNotFoundException.class, () -> service.getContext(bookingId, userId));
    }
}
