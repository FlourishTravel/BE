package com.flourishtravel.domain.flora.service;

import com.flourishtravel.common.exception.BadRequestException;
import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.chatbot.client.OpenMeteoClient;
import com.flourishtravel.domain.flora.dto.FloraLocationRequest;
import com.flourishtravel.domain.flora.dto.FloraLocationResponse;
import com.flourishtravel.domain.flora.entity.UserLocationPing;
import com.flourishtravel.domain.flora.repository.UserLocationPingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FloraLocationService {

    private final FloraPrivacyService privacyService;
    private final UserLocationPingRepository pingRepository;
    private final FloraJourneyService journeyService;
    private final FloraReminderService reminderService;
    private final OpenMeteoClient openMeteoClient;

    @Value("${app.flora.return-to-bus-threshold-meters:500}")
    private double returnThresholdMeters;

    @Value("${app.flora.return-to-bus-minutes-before:20}")
    private long returnMinutesBefore;

    @Transactional
    public FloraLocationResponse recordLocation(UUID bookingId, UUID userId, FloraLocationRequest request) {
        privacyService.requireLocationConsent(userId);
        validateCoordinates(request.getLatitude(), request.getLongitude());

        Booking booking = privacyService.requireOwnedBooking(bookingId, userId);
        Instant captured = request.getCapturedAt() != null ? request.getCapturedAt() : Instant.now();

        pingRepository.findTopByBookingIdAndUserIdOrderByCapturedAtDesc(bookingId, userId)
                .ifPresent(pingRepository::delete);

        UserLocationPing ping = UserLocationPing.builder()
                .booking(booking)
                .user(booking.getUser())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .accuracyMeters(request.getAccuracyMeters())
                .capturedAt(captured)
                .build();
        pingRepository.save(ping);

        Double distanceMeters = null;
        boolean suggestReturn = false;
        String message = "Flora đã cập nhật vị trí của bạn.";

        String meetingPoint = booking.getPickupAddress();
        if (meetingPoint != null && !meetingPoint.isBlank()) {
            double[] meetingCoords = openMeteoClient.geocode(meetingPoint + " Vietnam");
            if (meetingCoords != null) {
                distanceMeters = haversineMeters(request.getLatitude(), request.getLongitude(), meetingCoords[0], meetingCoords[1]);
                var journey = journeyService.getJourney(bookingId, userId);
                Long minutesUntil = journey.getMinutesUntilGathering();
                if (distanceMeters > returnThresholdMeters
                        && minutesUntil != null
                        && minutesUntil <= returnMinutesBefore
                        && minutesUntil >= 0) {
                    suggestReturn = true;
                    message = String.format(
                            "Bạn đang cách điểm tập trung khoảng %.0fm. Flora gợi ý bạn quay lại %s ngay để kịp giờ lên xe.",
                            distanceMeters, meetingPoint);
                    if (privacyService.hasNotificationConsent(userId)) {
                        reminderService.sendReturnToBusAlert(booking, userId, meetingPoint, distanceMeters);
                    }
                }
            }
        }

        return FloraLocationResponse.builder()
                .accepted(true)
                .distanceToMeetingMeters(distanceMeters)
                .returnToBusSuggested(suggestReturn)
                .message(message)
                .build();
    }

    static void validateCoordinates(Double lat, Double lon) {
        if (lat == null || lon == null) {
            throw new BadRequestException("latitude và longitude là bắt buộc");
        }
        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            throw new BadRequestException("Tọa độ GPS không hợp lệ");
        }
    }

    static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
