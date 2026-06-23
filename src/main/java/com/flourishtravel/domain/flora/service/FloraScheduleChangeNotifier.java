package com.flourishtravel.domain.flora.service;

import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.booking.repository.BookingRepository;
import com.flourishtravel.domain.flora.FloraReminderTypes;
import com.flourishtravel.domain.flora.entity.FloraReminderDelivery;
import com.flourishtravel.domain.flora.repository.FloraReminderDeliveryRepository;
import com.flourishtravel.domain.notification.entity.Notification;
import com.flourishtravel.domain.notification.service.NotificationService;
import com.flourishtravel.domain.tour.entity.TourSession;
import com.flourishtravel.domain.tour.entity.TourSessionActivityOverride;
import com.flourishtravel.domain.tour.schedule.SessionScheduleMergeHelper;
import com.flourishtravel.domain.tour.schedule.SessionScheduleMergeHelper.EffectiveActivityFields;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FloraScheduleChangeNotifier {

    private static final Set<String> ACTIVE_BOOKING = Set.of("paid", "confirmed");
    private static final List<String> MEETING_REMINDER_TYPES = List.of(
            FloraReminderTypes.TOUR_REMINDER_30_MINUTES,
            FloraReminderTypes.TOUR_REMINDER_15_MINUTES,
            FloraReminderTypes.TOUR_REMINDER_5_MINUTES,
            FloraReminderTypes.RETURN_TO_BUS_ALERT);

    private final BookingRepository bookingRepository;
    private final FloraPrivacyService privacyService;
    private final NotificationService notificationService;
    private final FloraReminderDeliveryRepository deliveryRepository;

    @Value("${app.flora.timezone:Asia/Ho_Chi_Minh}")
    private String tourTimezone;

    @Transactional
    public void notifyPublishedOverride(TourSession session, TourSessionActivityOverride override,
                                        EffectiveActivityFields before, EffectiveActivityFields after,
                                        LocalDate activityDate) {
        if (session == null || override == null || !isMaterialChange(before, after)) {
            return;
        }

        List<Booking> bookings = bookingRepository.findBySessionAndRosterStatusesWithGuests(session);
        String title = "Lịch trình tour đã được cập nhật";
        String body = buildBody(after);

        for (Booking booking : bookings) {
            if (!isActiveTrip(booking)) continue;
            invalidateOutdatedMeetingReminders(booking, before, activityDate);
            UUID userId = booking.getUser().getId();
            if (!privacyService.hasNotificationConsent(userId)) continue;

            String key = scheduleChangedKey(booking.getId(), session.getId(),
                    override.getTourActivity().getId(), override.getVersion());
            if (deliveryRepository.existsByIdempotencyKey(key)) continue;

            Notification n = notificationService.createFloraNotification(
                    userId, FloraReminderTypes.SCHEDULE_CHANGED, title, body, booking.getId());

            FloraReminderDelivery delivery = FloraReminderDelivery.builder()
                    .booking(booking)
                    .user(booking.getUser())
                    .reminderType(FloraReminderTypes.SCHEDULE_CHANGED)
                    .scheduledAt(Instant.now())
                    .sentAt(Instant.now())
                    .status("sent")
                    .notificationId(n.getId())
                    .idempotencyKey(key)
                    .build();
            deliveryRepository.save(delivery);
        }
    }

    static String scheduleChangedKey(UUID bookingId, UUID sessionId, UUID activityId, int version) {
        return bookingId + ":" + FloraReminderTypes.SCHEDULE_CHANGED + ":" + sessionId + ":" + activityId + ":" + version;
    }

    public static boolean isMaterialChange(EffectiveActivityFields before, EffectiveActivityFields after) {
        if (after == null) return false;
        if (after.isCancelled()) return true;
        if (before == null) return after.isFromPublishedOverride();
        return !Objects.equals(before.getStartTime(), after.getStartTime())
                || !Objects.equals(before.getEndTime(), after.getEndTime())
                || !Objects.equals(norm(before.getLocationName()), norm(after.getLocationName()))
                || !Objects.equals(norm(before.getLocationAddress()), norm(after.getLocationAddress()))
                || !Objects.equals(before.getScheduleStatus(), after.getScheduleStatus())
                || before.isGatheringEvent() != after.isGatheringEvent()
                || !Objects.equals(before.getGatheringEventType(), after.getGatheringEventType())
                || before.isCancelled() != after.isCancelled();
    }

    private static String norm(String s) {
        return s != null ? s.trim() : null;
    }

    private static String buildBody(EffectiveActivityFields after) {
        StringBuilder sb = new StringBuilder(
                "Flora thông báo: thời gian hoặc điểm tập trung đã thay đổi. Bạn vui lòng kiểm tra hành trình mới nhất.");
        if (after.isCancelled()) {
            return sb.toString();
        }
        if (after.isGatheringEvent()
                && com.flourishtravel.domain.flora.FloraScheduleConstants.SCHEDULE_CONFIRMED.equals(after.getScheduleStatus())
                && after.getStartTime() != null
                && after.getLocationName() != null && !after.getLocationName().isBlank()) {
            sb.append(" Tập trung lúc ")
                    .append(String.format("%02d:%02d", after.getStartTime().getHour(), after.getStartTime().getMinute()))
                    .append(" tại ")
                    .append(after.getLocationName().trim())
                    .append(".");
        }
        return sb.toString();
    }

    private boolean isActiveTrip(Booking booking) {
        if (booking == null || booking.getStatus() == null) return false;
        if (!ACTIVE_BOOKING.contains(booking.getStatus().toLowerCase())) return false;
        TourSession session = booking.getSession();
        if (session == null || session.getStartDate() == null) return false;
        ZoneId zone = ZoneId.of(tourTimezone);
        LocalDate today = LocalDate.now(zone);
        return !today.isBefore(session.getStartDate())
                && (session.getEndDate() == null || !today.isAfter(session.getEndDate()));
    }

    private void invalidateOutdatedMeetingReminders(
            Booking booking, EffectiveActivityFields before, LocalDate activityDate) {
        if (before == null || activityDate == null) return;
        if (!before.isGatheringEvent() || before.getStartTime() == null) return;
        ZoneId zone = ZoneId.of(tourTimezone);
        Instant oldMeeting = before.getStartTime()
                .atDate(activityDate)
                .atZone(zone)
                .toInstant();
        for (String type : MEETING_REMINDER_TYPES) {
            String key = FloraReminderService.meetingReminderKey(booking.getId(), type, oldMeeting);
            deliveryRepository.findByIdempotencyKey(key).ifPresent(deliveryRepository::delete);
        }
    }
}
