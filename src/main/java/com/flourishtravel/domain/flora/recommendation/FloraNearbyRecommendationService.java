package com.flourishtravel.domain.flora.recommendation;

import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.chatbot.client.OverpassClient;
import com.flourishtravel.domain.chatbot.service.ChatbotDataService;
import com.flourishtravel.domain.flora.FloraRecommendationConstants;
import com.flourishtravel.domain.flora.FloraScheduleConstants;
import com.flourishtravel.domain.flora.dto.FloraJourneyDto;
import com.flourishtravel.domain.flora.dto.FloraNextMeetingDto;
import com.flourishtravel.domain.flora.entity.UserTravelPreference;
import com.flourishtravel.domain.flora.recommendation.dto.FloraNearbyRecommendationRequest;
import com.flourishtravel.domain.flora.recommendation.dto.FloraNearbyRecommendationResponse;
import com.flourishtravel.domain.flora.recommendation.dto.FloraNearbyRecommendationResponse.JourneyContextDto;
import com.flourishtravel.domain.flora.recommendation.dto.FloraNearbyRecommendationResponse.MapActionDto;
import com.flourishtravel.domain.flora.recommendation.dto.FloraNearbyRecommendationResponse.RecommendationItemDto;
import com.flourishtravel.domain.flora.service.FloraJourneyService;
import com.flourishtravel.domain.flora.service.FloraLocationService;
import com.flourishtravel.domain.flora.service.FloraPrivacyService;
import com.flourishtravel.domain.tour.entity.Tour;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FloraNearbyRecommendationService {

    private final FloraPrivacyService privacyService;
    private final FloraJourneyService journeyService;
    private final FloraRecommendationContextBuilder contextBuilder;
    private final FloraRecommendationProperties properties;
    private final OverpassClient overpassClient;
    private final ChatbotDataService chatbotDataService;

    @Transactional(readOnly = true)
    public FloraNearbyRecommendationResponse recommend(
            UUID bookingId, UUID userId, FloraNearbyRecommendationRequest request) {
        contextBuilder.validateRequest(request, properties);
        Booking booking = privacyService.requireOwnedBooking(bookingId, userId);
        FloraJourneyDto journey = journeyService.getJourney(bookingId, userId);

        FloraRecommendationContextBuilder.ResolvedLocation location =
                contextBuilder.resolveLocation(userId, request, journey, booking);

        List<String> warnings = new ArrayList<>();
        FloraPoiScoringService.ScheduleContext schedule = FloraPoiScoringService.fromJourney(journey);
        if (!schedule.isCanValidateSchedule()) {
            warnings.add(FloraRecommendationConstants.SCHEDULE_UNCONFIRMED_WARNING);
        }

        JourneyContextDto journeyContext = buildJourneyContext(journey, schedule);

        if (FloraRecommendationConstants.LOCATION_UNAVAILABLE.equals(location.getSource())
                || location.getLatitude() == null || location.getLongitude() == null) {
            warnings.add("Flora chưa xác định được vị trí để gợi ý địa điểm gần bạn.");
            return FloraNearbyRecommendationResponse.builder()
                    .bookingId(bookingId)
                    .locationSource(location.getSource())
                    .locationLabel(location.getLabel())
                    .journeyContext(journeyContext)
                    .recommendations(List.of())
                    .warnings(warnings)
                    .build();
        }

        int radius = request.getRadiusMeters() != null ? request.getRadiusMeters() : properties.getDefaultRadiusMeters();
        int limit = request.getLimit() != null ? request.getLimit() : properties.getDefaultLimit();
        List<String> categories = request.getCategories() != null && !request.getCategories().isEmpty()
                ? request.getCategories()
                : List.of("RESTAURANT", "CAFE", "ATTRACTION");

        UserTravelPreference pref = privacyService.getPreferencesOrDefault(userId);
        boolean personalize = privacyService.hasPersonalizationConsent(userId);

        appendWeatherWarnings(warnings, booking, location.getLatitude(), location.getLongitude());

        List<Map<String, Object>> pois = overpassClient.findNearbyPois(
                location.getLatitude(), location.getLongitude(), radius, categories, limit * 3);

        List<ScoredPoi> scored = new ArrayList<>();
        for (Map<String, Object> poi : pois) {
            ScoredPoi item = scorePoi(poi, location, schedule, pref, personalize, categories);
            if (item == null) continue;
            scored.add(item);
        }

        scored.sort(Comparator
                .comparing(ScoredPoi::fitsSchedule).reversed()
                .thenComparingInt(ScoredPoi::distanceMeters));

        List<RecommendationItemDto> recommendations = scored.stream()
                .limit(limit)
                .map(ScoredPoi::dto)
                .toList();

        if (recommendations.isEmpty()) {
            warnings.add("Chưa tìm thấy địa điểm phù hợp trong bán kính đã chọn.");
        }

        log.info("event=flora_nearby_recommend booking_id_hash={} location_source={} count={}",
                hashId(bookingId), location.getSource(), recommendations.size());

        return FloraNearbyRecommendationResponse.builder()
                .bookingId(bookingId)
                .locationSource(location.getSource())
                .locationLabel(location.getLabel())
                .journeyContext(journeyContext)
                .recommendations(recommendations)
                .warnings(warnings.isEmpty() ? List.of() : List.copyOf(warnings))
                .build();
    }

    private ScoredPoi scorePoi(
            Map<String, Object> poi,
            FloraRecommendationContextBuilder.ResolvedLocation origin,
            FloraPoiScoringService.ScheduleContext schedule,
            UserTravelPreference pref,
            boolean personalize,
            List<String> requestedCategories) {

        @SuppressWarnings("unchecked")
        Map<String, Object> tags = poi.get("tags") instanceof Map<?, ?> t
                ? (Map<String, Object>) t : Map.of();
        Object nameObj = tags.get("name");
        if (nameObj == null || nameObj.toString().isBlank()) return null;
        String name = nameObj.toString();
        double[] coords = extractCoords(poi);
        if (coords == null) return null;

        int distance = (int) Math.round(FloraGeoUtils.haversineMeters(
                origin.getLatitude(), origin.getLongitude(), coords[0], coords[1]));
        String category = FloraPoiScoringService.mapOsmCategory(tags);
        if (!categoryMatchesRequest(category, requestedCategories)) return null;

        int visit = FloraPoiScoringService.visitMinutesForCategory(category, properties);
        int roundTrip = FloraPoiScoringService.estimateRoundTripMinutes(
                distance, properties.getEstimatedWalkingSpeedMetersPerMinute());
        boolean fits = FloraPoiScoringService.fitsSchedule(schedule, roundTrip, visit);

        String foodMatch = FloraPoiScoringService.resolveFoodMatch(name, category, pref, personalize);
        String budgetMatch = FloraPoiScoringService.resolveBudgetMatch(pref, personalize);
        if (FloraRecommendationConstants.FOOD_EXCLUDED.equals(foodMatch)) return null;

        List<String> itemWarnings = new ArrayList<>(
                FloraPoiScoringService.baseWarnings(true, isFoodCategory(category)));

        String id = "osm:" + poi.get("type") + ":" + poi.get("id");
        String address = tags.get("addr:street") != null ? tags.get("addr:street").toString() : null;

        RecommendationItemDto dto = RecommendationItemDto.builder()
                .id(id)
                .name(name)
                .category(category)
                .address(address)
                .latitude(coords[0])
                .longitude(coords[1])
                .dataSource(FloraRecommendationConstants.SOURCE_OSM)
                .straightLineDistanceMeters(distance)
                .estimatedVisitMinutes(visit)
                .estimatedRoundTripMinutes(roundTrip)
                .fitsSchedule(fits)
                .foodMatchStatus(foodMatch)
                .budgetMatchStatus(budgetMatch)
                .reason(buildReason(fits, distance, schedule))
                .warnings(itemWarnings)
                .mapAction(MapActionDto.builder()
                        .type("OPEN_MAP")
                        .latitude(coords[0])
                        .longitude(coords[1])
                        .build())
                .build();

        return new ScoredPoi(distance, fits, dto);
    }

    private static boolean categoryMatchesRequest(String category, List<String> requested) {
        if (requested == null || requested.isEmpty()) return true;
        return requested.stream()
                .anyMatch(r -> r != null && r.equalsIgnoreCase(category));
    }

    private static boolean isFoodCategory(String category) {
        return "RESTAURANT".equalsIgnoreCase(category) || "CAFE".equalsIgnoreCase(category);
    }

    private static String buildReason(boolean fits, int distanceMeters, FloraPoiScoringService.ScheduleContext schedule) {
        if (fits) {
            return "Gần vị trí hiện tại và phù hợp với thời gian còn lại";
        }
        if (!schedule.isCanValidateSchedule()) {
            return "Gần vị trí hiện tại — chưa xác nhận đủ thời gian trước giờ tập trung";
        }
        return "Cách khoảng " + distanceMeters + "m — có thể không đủ thời gian trước giờ tập trung";
    }

    private static double[] extractCoords(Map<String, Object> poi) {
        Object lat = poi.get("lat");
        Object lon = poi.get("lon");
        if (lat instanceof Number la && lon instanceof Number lo) {
            return new double[] {la.doubleValue(), lo.doubleValue()};
        }
        Object center = poi.get("center");
        if (center instanceof Map<?, ?> c) {
            Object clat = c.get("lat");
            Object clon = c.get("lon");
            if (clat instanceof Number la && clon instanceof Number lo) {
                return new double[] {la.doubleValue(), lo.doubleValue()};
            }
        }
        return null;
    }

    private JourneyContextDto buildJourneyContext(
            FloraJourneyDto journey, FloraPoiScoringService.ScheduleContext schedule) {
        FloraNextMeetingDto meeting = journey != null ? journey.getNextMeeting() : null;
        return JourneyContextDto.builder()
                .currentActivityTitle(journey != null && journey.getCurrentActivity() != null
                        ? journey.getCurrentActivity().getTitle() : null)
                .nextMeetingTime(meeting != null ? meeting.getTime() : null)
                .nextMeetingLocation(meeting != null ? meeting.getLocationName() : null)
                .scheduleStatus(meeting != null ? meeting.getScheduleStatus() : null)
                .freeMinutesUntilMeeting(journey != null ? journey.getFreeMinutesUntilMeeting() : null)
                .canValidateSchedule(schedule.isCanValidateSchedule())
                .build();
    }

    private void appendWeatherWarnings(List<String> warnings, Booking booking, double lat, double lon) {
        try {
            Tour tour = booking.getSession() != null ? booking.getSession().getTour() : null;
            String dest = tour != null ? tour.getDestinationCity() : null;
            if (dest == null || dest.isBlank()) return;
            var weather = chatbotDataService.getWeatherForecast(dest);
            if (weather == null || weather.getSummary() == null) return;
            String summary = weather.getSummary().toLowerCase(Locale.ROOT);
            if (summary.contains("mưa")) {
                warnings.add("Dự báo có mưa — ưu tiên địa điểm trong nhà hoặc mang ô.");
            } else if (summary.contains("nắng") || summary.contains("nóng")) {
                warnings.add("Thời tiết nắng — nhớ mang nước và chống nắng.");
            }
        } catch (Exception e) {
            log.debug("Weather hint skipped: {}", e.getMessage());
        }
    }

    private static String hashId(UUID id) {
        return id == null ? "none" : Integer.toHexString(id.hashCode());
    }

    private record ScoredPoi(int distanceMeters, boolean fitsSchedule, RecommendationItemDto dto) {}
}
