package com.flourishtravel.domain.flora.service;

import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.booking.repository.BookingRepository;
import com.flourishtravel.domain.flora.FloraReminderTypes;
import com.flourishtravel.domain.flora.entity.FloraReminderDelivery;
import com.flourishtravel.domain.flora.repository.FloraReminderDeliveryRepository;
import com.flourishtravel.domain.flora.repository.UserLocationPingRepository;
import com.flourishtravel.domain.notification.entity.Notification;
import com.flourishtravel.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FloraReminderService {

    private static final Set<String> ELIGIBLE = Set.of("paid", "confirmed");
    private static final int[] REMINDER_MINUTES = {30, 15, 5};

    private final BookingRepository bookingRepository;
    private final FloraReminderDeliveryRepository deliveryRepository;
    private final FloraJourneyService journeyService;
    private final FloraPrivacyService privacyService;
    private final NotificationService notificationService;
    private final UserLocationPingRepository locationPingRepository;

    @Value("${app.flora.timezone:Asia/Ho_Chi_Minh}")
    private String tourTimezone;

    @Value("${app.flora.location-retention-hours:48}")
    private int locationRetentionHours;

    @Scheduled(fixedRateString = "${app.flora.reminder-check-ms:60000}")
    @Transactional
    public void runReminderJob() {
        cleanupOldLocations();
        Instant now = Instant.now();
        ZoneId zone = ZoneId.of(tourTimezone);
        LocalDate today = LocalDate.now(zone);

        for (Booking booking : bookingRepository.findActiveForFloraReminders(today, ELIGIBLE)) {
            UUID userId = booking.getUser().getId();
            if (!privacyService.hasNotificationConsent(userId)) continue;
            if (!journeyService.isActiveTrip(booking)) continue;

            Instant gathering = journeyService.computeNextGathering(booking.getSession(), zone);
            if (gathering == null) continue;

            long minutesUntil = Duration.between(now, gathering).toMinutes();
            for (int threshold : REMINDER_MINUTES) {
                if (minutesUntil <= threshold && minutesUntil >= threshold - 1) {
                    String type = switch (threshold) {
                        case 30 -> FloraReminderTypes.TOUR_REMINDER_30_MINUTES;
                        case 15 -> FloraReminderTypes.TOUR_REMINDER_15_MINUTES;
                        default -> FloraReminderTypes.TOUR_REMINDER_5_MINUTES;
                    };
                    deliverOnce(booking, userId, type, gathering, buildGatheringMessage(threshold, booking));
                }
            }
        }

        checkPostTourFeedback(today);
    }

    private void checkPostTourFeedback(LocalDate today) {
        for (Booking booking : bookingRepository.findRecentlyCompletedForFlora(today.minusDays(1), Set.of("completed"))) {
            UUID userId = booking.getUser().getId();
            if (!privacyService.hasNotificationConsent(userId)) continue;
            String key = idempotencyKey(booking.getId(), FloraReminderTypes.POST_TOUR_FEEDBACK, Instant.EPOCH);
            if (deliveryRepository.existsByIdempotencyKey(key)) continue;
            deliverOnce(booking, userId, FloraReminderTypes.POST_TOUR_FEEDBACK, Instant.now(),
                    "Chuyến đi của bạn đã kết thúc rồi. Flora rất muốn biết bạn thích nhất điều gì để lần sau gợi ý phù hợp hơn.");
        }
    }

    @Transactional
    public void sendReturnToBusAlert(Booking booking, UUID userId, String meetingPoint, double distanceMeters) {
        Instant gather = journeyService.computeNextGathering(booking.getSession(), ZoneId.of(tourTimezone));
        String key = idempotencyKey(booking.getId(), FloraReminderTypes.RETURN_TO_BUS_ALERT,
                gather != null ? gather : Instant.now());
        if (deliveryRepository.existsByIdempotencyKey(key)) return;
        String body = String.format(
                "Bạn đang cách điểm tập trung khoảng %.0fm. Flora gợi ý bạn quay lại %s ngay để kịp giờ lên xe.",
                distanceMeters, meetingPoint);
        deliverOnce(booking, userId, FloraReminderTypes.RETURN_TO_BUS_ALERT, Instant.now(), body);
    }

    private void deliverOnce(Booking booking, UUID userId, String type, Instant scheduledAt, String body) {
        String key = idempotencyKey(booking.getId(), type, scheduledAt);
        if (deliveryRepository.existsByIdempotencyKey(key)) return;

        Notification n = notificationService.createFloraNotification(userId, type, "Flora AI", body, booking.getId());
        FloraReminderDelivery delivery = FloraReminderDelivery.builder()
                .booking(booking)
                .user(booking.getUser())
                .reminderType(type)
                .scheduledAt(scheduledAt)
                .sentAt(Instant.now())
                .status("sent")
                .notificationId(n.getId())
                .idempotencyKey(key)
                .build();
        deliveryRepository.save(delivery);
    }

    static String idempotencyKey(UUID bookingId, String type, Instant at) {
        long bucket = at.getEpochSecond() / 60;
        return bookingId + ":" + type + ":" + bucket;
    }

    private static String buildGatheringMessage(int minutes, Booking booking) {
        String point = booking.getPickupAddress() != null ? booking.getPickupAddress() : "điểm tập trung";
        if (minutes <= 5) {
            return "Flora nhắc gấp: còn khoảng 5 phút nữa đoàn tập trung tại " + point + ".";
        }
        if (minutes <= 15) {
            return "Flora nhắc bạn nhé: còn khoảng 15 phút nữa đoàn sẽ tập trung lên xe tại " + point + ". Bạn nên bắt đầu quay lại để không lỡ lịch trình.";
        }
        return "Flora nhắc nhẹ: còn khoảng 30 phút nữa đến giờ tập trung tại " + point + ".";
    }

    @Transactional
    public void cleanupOldLocations() {
        Instant before = Instant.now().minus(Duration.ofHours(locationRetentionHours));
        locationPingRepository.deleteByCapturedAtBefore(before);
    }
}
