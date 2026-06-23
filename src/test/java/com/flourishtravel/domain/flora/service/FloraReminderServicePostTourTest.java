package com.flourishtravel.domain.flora.service;

import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.booking.repository.BookingRepository;
import com.flourishtravel.domain.flora.FloraReminderTypes;
import com.flourishtravel.domain.flora.repository.FloraReminderDeliveryRepository;
import com.flourishtravel.domain.flora.repository.UserLocationPingRepository;
import com.flourishtravel.domain.notification.entity.Notification;
import com.flourishtravel.domain.notification.service.NotificationService;
import com.flourishtravel.domain.review.repository.ReviewRepository;
import com.flourishtravel.domain.tour.entity.TourSession;
import com.flourishtravel.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FloraReminderServicePostTourTest {

    @Mock BookingRepository bookingRepository;
    @Mock FloraReminderDeliveryRepository deliveryRepository;
    @Mock FloraJourneyService journeyService;
    @Mock FloraPrivacyService privacyService;
    @Mock NotificationService notificationService;
    @Mock UserLocationPingRepository locationPingRepository;
    @Mock ReviewRepository reviewRepository;
    @InjectMocks FloraReminderService reminderService;

    private UUID bookingId;
    private UUID userId;
    private Booking booking;

    @BeforeEach
    void setUp() throws Exception {
        bookingId = UUID.randomUUID();
        userId = UUID.randomUUID();
        User user = User.builder().build();
        user.setId(userId);
        booking = Booking.builder()
                .user(user)
                .session(TourSession.builder().endDate(LocalDate.of(2026, 6, 22)).build())
                .status("completed")
                .build();
        booking.setId(bookingId);

        var tz = FloraReminderService.class.getDeclaredField("tourTimezone");
        tz.setAccessible(true);
        tz.set(reminderService, "Asia/Ho_Chi_Minh");
    }

    @Test
    void postTourReminder_skipsWhenReviewExists() throws Exception {
        LocalDate today = LocalDate.of(2026, 6, 23);
        when(bookingRepository.findRecentlyCompletedForFlora(today.minusDays(1), Set.of("completed")))
                .thenReturn(List.of(booking));
        when(privacyService.hasNotificationConsent(userId)).thenReturn(true);
        when(reviewRepository.existsByBooking_Id(bookingId)).thenReturn(true);

        invokeCheckPostTourFeedback(today);

        verify(notificationService, never()).createFloraNotification(any(), any(), any(), any(), any());
    }

    @Test
    void postTourReminder_sentOnceWithSpecCopy() throws Exception {
        LocalDate today = LocalDate.of(2026, 6, 23);
        when(bookingRepository.findRecentlyCompletedForFlora(today.minusDays(1), Set.of("completed")))
                .thenReturn(List.of(booking));
        when(privacyService.hasNotificationConsent(userId)).thenReturn(true);
        when(reviewRepository.existsByBooking_Id(bookingId)).thenReturn(false);
        when(deliveryRepository.existsByIdempotencyKey(anyString())).thenReturn(false);
        Notification notification = Notification.builder().build();
        notification.setId(UUID.randomUUID());
        when(notificationService.createFloraNotification(eq(userId), eq(FloraReminderTypes.POST_TOUR_FEEDBACK),
                eq("Flora muốn nghe cảm nhận của bạn"), anyString(), eq(bookingId)))
                .thenReturn(notification);

        invokeCheckPostTourFeedback(today);

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).createFloraNotification(
                eq(userId), eq(FloraReminderTypes.POST_TOUR_FEEDBACK),
                eq("Flora muốn nghe cảm nhận của bạn"), bodyCaptor.capture(), eq(bookingId));
        assertTrue(bodyCaptor.getValue().contains("Chia sẻ vài điều"));
        verify(deliveryRepository).save(any());
    }

    @Test
    void postTourReminder_skipsWithoutNotificationConsent() throws Exception {
        LocalDate today = LocalDate.of(2026, 6, 23);
        when(bookingRepository.findRecentlyCompletedForFlora(today.minusDays(1), Set.of("completed")))
                .thenReturn(List.of(booking));
        when(privacyService.hasNotificationConsent(userId)).thenReturn(false);

        invokeCheckPostTourFeedback(today);

        verify(notificationService, never()).createFloraNotification(any(), any(), any(), any(), any());
    }

    private void invokeCheckPostTourFeedback(LocalDate today) throws Exception {
        Method m = FloraReminderService.class.getDeclaredMethod("checkPostTourFeedback", LocalDate.class);
        m.setAccessible(true);
        m.invoke(reminderService, today);
    }
}
