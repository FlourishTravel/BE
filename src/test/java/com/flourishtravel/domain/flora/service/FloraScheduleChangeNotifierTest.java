package com.flourishtravel.domain.flora.service;

import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.booking.repository.BookingRepository;
import com.flourishtravel.domain.flora.FloraReminderTypes;
import com.flourishtravel.domain.flora.repository.FloraReminderDeliveryRepository;
import com.flourishtravel.domain.notification.service.NotificationService;
import com.flourishtravel.domain.tour.entity.TourActivity;
import com.flourishtravel.domain.tour.entity.TourSession;
import com.flourishtravel.domain.tour.entity.TourSessionActivityOverride;
import com.flourishtravel.domain.tour.schedule.SessionScheduleMergeHelper.EffectiveActivityFields;
import com.flourishtravel.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FloraScheduleChangeNotifierTest {

    @Mock BookingRepository bookingRepository;
    @Mock FloraPrivacyService privacyService;
    @Mock NotificationService notificationService;
    @Mock FloraReminderDeliveryRepository deliveryRepository;
    @InjectMocks FloraScheduleChangeNotifier notifier;

    private TourSession session;
    private TourSessionActivityOverride override;
    private Booking booking;
    private UUID userId;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notifier, "tourTimezone", "Asia/Ho_Chi_Minh");
        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate today = LocalDate.now(zone);
        session = TourSession.builder().startDate(today.minusDays(1)).endDate(today.plusDays(2)).build();
        session.setId(UUID.randomUUID());
        TourActivity act = TourActivity.builder().build();
        act.setId(UUID.randomUUID());
        override = TourSessionActivityOverride.builder().tourActivity(act).version(3).build();
        userId = UUID.randomUUID();
        User user = User.builder().build();
        user.setId(userId);
        booking = Booking.builder().user(user).session(session).status("confirmed").build();
        booking.setId(UUID.randomUUID());
    }

    @Test
    void scheduleChangedKey_includesVersion() {
        UUID bookingId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        String key = FloraScheduleChangeNotifier.scheduleChangedKey(bookingId, sessionId, activityId, 3);
        assertEquals(bookingId + ":" + FloraReminderTypes.SCHEDULE_CHANGED + ":" + sessionId + ":" + activityId + ":3", key);
    }

    @Test
    void isMaterialChange_detectsTimeShift() {
        EffectiveActivityFields before = EffectiveActivityFields.builder()
                .startTime(LocalTime.of(10, 40))
                .locationName("A")
                .gatheringEvent(true)
                .scheduleStatus("CONFIRMED")
                .build();
        EffectiveActivityFields after = EffectiveActivityFields.builder()
                .startTime(LocalTime.of(10, 20))
                .locationName("A")
                .gatheringEvent(true)
                .scheduleStatus("CONFIRMED")
                .fromPublishedOverride(true)
                .build();
        assertTrue(FloraScheduleChangeNotifier.isMaterialChange(before, after));
    }

    @Test
    void isMaterialChange_unchangedIsFalse() {
        EffectiveActivityFields same = EffectiveActivityFields.builder()
                .startTime(LocalTime.of(10, 40))
                .locationName("A")
                .gatheringEvent(true)
                .scheduleStatus("CONFIRMED")
                .build();
        assertFalse(FloraScheduleChangeNotifier.isMaterialChange(same, same));
    }

    @Test
    void notify_skipsWhenIdempotencyKeyExists() {
        EffectiveActivityFields before = EffectiveActivityFields.builder()
                .startTime(LocalTime.of(10, 40))
                .locationName("A")
                .gatheringEvent(true)
                .scheduleStatus("CONFIRMED")
                .build();
        EffectiveActivityFields after = EffectiveActivityFields.builder()
                .startTime(LocalTime.of(10, 20))
                .locationName("B")
                .gatheringEvent(true)
                .scheduleStatus("CONFIRMED")
                .fromPublishedOverride(true)
                .build();

        when(bookingRepository.findBySessionAndRosterStatusesWithGuests(session)).thenReturn(List.of(booking));
        when(privacyService.hasNotificationConsent(userId)).thenReturn(true);
        when(deliveryRepository.existsByIdempotencyKey(anyString())).thenReturn(true);

        notifier.notifyPublishedOverride(session, override, before, after, LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")));

        verify(notificationService, never()).createFloraNotification(any(), any(), any(), any(), any());
    }

    @Test
    void notify_sendsOnceWhenConsentAndMaterial() {
        EffectiveActivityFields before = EffectiveActivityFields.builder()
                .startTime(LocalTime.of(10, 40))
                .locationName("A")
                .gatheringEvent(true)
                .scheduleStatus("CONFIRMED")
                .build();
        EffectiveActivityFields after = EffectiveActivityFields.builder()
                .startTime(LocalTime.of(10, 20))
                .locationName("B")
                .gatheringEvent(true)
                .scheduleStatus("CONFIRMED")
                .fromPublishedOverride(true)
                .build();

        when(bookingRepository.findBySessionAndRosterStatusesWithGuests(session)).thenReturn(List.of(booking));
        when(privacyService.hasNotificationConsent(userId)).thenReturn(true);
        when(deliveryRepository.existsByIdempotencyKey(anyString())).thenReturn(false);
        when(notificationService.createFloraNotification(eq(userId), eq(FloraReminderTypes.SCHEDULE_CHANGED),
                anyString(), anyString(), eq(booking.getId()))).thenReturn(
                com.flourishtravel.domain.notification.entity.Notification.builder().build());

        notifier.notifyPublishedOverride(session, override, before, after, LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")));

        verify(notificationService, times(1)).createFloraNotification(any(), any(), any(), any(), any());
        verify(deliveryRepository, times(1)).save(any());
    }
}
