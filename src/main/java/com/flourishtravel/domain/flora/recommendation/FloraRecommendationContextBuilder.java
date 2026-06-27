package com.flourishtravel.domain.flora.recommendation;

import com.flourishtravel.common.exception.BadRequestException;
import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.chatbot.client.OpenMeteoClient;
import com.flourishtravel.domain.flora.FloraRecommendationConstants;
import com.flourishtravel.domain.flora.dto.FloraActivityDto;
import com.flourishtravel.domain.flora.dto.FloraJourneyDto;
import com.flourishtravel.domain.flora.recommendation.dto.FloraNearbyRecommendationRequest;
import com.flourishtravel.domain.flora.service.FloraPrivacyService;
import com.flourishtravel.domain.tour.entity.Tour;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FloraRecommendationContextBuilder {

    private final FloraPrivacyService privacyService;
    private final OpenMeteoClient openMeteoClient;

    public void validateRequest(FloraNearbyRecommendationRequest request, FloraRecommendationProperties props) {
        if (request.getLatitude() != null || request.getLongitude() != null) {
            if (request.getLatitude() == null || request.getLongitude() == null) {
                throw new BadRequestException("latitude và longitude phải được gửi cùng nhau.");
            }
            double lat = request.getLatitude();
            double lon = request.getLongitude();
            if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
                throw new BadRequestException("Tọa độ GPS không hợp lệ.");
            }
        }
        int radius = request.getRadiusMeters() != null ? request.getRadiusMeters() : props.getDefaultRadiusMeters();
        if (radius < 100 || radius > props.getMaxRadiusMeters()) {
            throw new BadRequestException("radiusMeters phải từ 100 đến " + props.getMaxRadiusMeters() + ".");
        }
        int limit = request.getLimit() != null ? request.getLimit() : props.getDefaultLimit();
        if (limit < 1 || limit > props.getMaxLimit()) {
            throw new BadRequestException("limit phải từ 1 đến " + props.getMaxLimit() + ".");
        }
    }

    public ResolvedLocation resolveLocation(
            java.util.UUID userId,
            FloraNearbyRecommendationRequest request,
            FloraJourneyDto journey,
            Booking booking) {

        boolean hasGps = request.getLatitude() != null && request.getLongitude() != null;
        boolean profileConsent = privacyService.hasLocationConsent(userId);
        boolean requestConsent = Boolean.TRUE.equals(request.getLocationConsent());

        if (hasGps && (profileConsent || requestConsent)) {
            return ResolvedLocation.builder()
                    .latitude(request.getLatitude())
                    .longitude(request.getLongitude())
                    .source(FloraRecommendationConstants.LOCATION_USER)
                    .label(journey != null && journey.getCurrentActivity() != null
                            ? "Gần " + journey.getCurrentActivity().getLocationName()
                            : "Vị trí hiện tại của bạn")
                    .build();
        }

        FloraActivityDto current = journey != null ? journey.getCurrentActivity() : null;
        FloraActivityDto next = journey != null ? journey.getNextActivity() : null;
        var meeting = journey != null ? journey.getNextMeeting() : null;

        ResolvedLocation fromCoords = coordsFromActivity(current, FloraRecommendationConstants.LOCATION_ACTIVITY);
        if (fromCoords != null) return fromCoords;

        fromCoords = coordsFromActivity(next, FloraRecommendationConstants.LOCATION_ACTIVITY);
        if (fromCoords != null) return fromCoords;

        if (meeting != null && meeting.getLatitude() != null && meeting.getLongitude() != null) {
            return ResolvedLocation.builder()
                    .latitude(meeting.getLatitude())
                    .longitude(meeting.getLongitude())
                    .source(FloraRecommendationConstants.LOCATION_ACTIVITY)
                    .label(meeting.getLocationName() != null ? meeting.getLocationName() : "Điểm tập trung")
                    .build();
        }

        ResolvedLocation geocoded = geocodeNamedPlace(current != null ? current.getLocationName() : null);
        if (geocoded != null) return geocoded;

        geocoded = geocodeNamedPlace(next != null ? next.getLocationName() : null);
        if (geocoded != null) return geocoded;

        geocoded = geocodeNamedPlace(meeting != null ? meeting.getLocationName() : null);
        if (geocoded != null) return geocoded;

        if (current != null && current.getLocationAddress() != null && !current.getLocationAddress().isBlank()) {
            geocoded = geocodeNamedPlace(current.getLocationAddress());
            if (geocoded != null) return geocoded;
        }

        Tour tour = booking.getSession() != null ? booking.getSession().getTour() : null;
        String dest = tour != null ? tour.getDestinationCity() : null;
        if (dest != null && !dest.isBlank()) {
            double[] coords = openMeteoClient.geocode(dest + " Vietnam");
            if (coords != null) {
                return ResolvedLocation.builder()
                        .latitude(coords[0])
                        .longitude(coords[1])
                        .source(FloraRecommendationConstants.LOCATION_DESTINATION)
                        .label(dest)
                        .build();
            }
        }

        return ResolvedLocation.builder()
                .source(FloraRecommendationConstants.LOCATION_UNAVAILABLE)
                .label(null)
                .build();
    }

    private static ResolvedLocation coordsFromActivity(FloraActivityDto activity, String source) {
        if (activity == null || activity.getLatitude() == null || activity.getLongitude() == null) {
            return null;
        }
        return ResolvedLocation.builder()
                .latitude(activity.getLatitude())
                .longitude(activity.getLongitude())
                .source(source)
                .label(activity.getLocationName() != null ? activity.getLocationName() : "Điểm hoạt động")
                .build();
    }

    private ResolvedLocation geocodeNamedPlace(String placeName) {
        if (placeName == null || placeName.isBlank()) return null;
        double[] coords = openMeteoClient.geocode(placeName.trim() + " Vietnam");
        if (coords == null) return null;
        return ResolvedLocation.builder()
                .latitude(coords[0])
                .longitude(coords[1])
                .source(FloraRecommendationConstants.LOCATION_DESTINATION)
                .label(placeName.trim())
                .build();
    }

    @Value
    @Builder
    public static class ResolvedLocation {
        Double latitude;
        Double longitude;
        String source;
        String label;
    }
}
