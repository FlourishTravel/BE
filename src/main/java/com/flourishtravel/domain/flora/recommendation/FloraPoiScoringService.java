package com.flourishtravel.domain.flora.recommendation;

import com.flourishtravel.domain.flora.FloraRecommendationConstants;
import com.flourishtravel.domain.flora.FloraScheduleConstants;
import com.flourishtravel.domain.flora.dto.FloraJourneyDto;
import com.flourishtravel.domain.flora.dto.FloraNextMeetingDto;
import com.flourishtravel.domain.flora.entity.UserTravelPreference;
import lombok.Builder;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class FloraPoiScoringService {

    private FloraPoiScoringService() {}

    @Value
    @Builder
    public static class ScheduleContext {
        boolean canValidateSchedule;
        Long freeMinutesUntilMeeting;
        String scheduleStatus;
    }

    public static ScheduleContext fromJourney(FloraJourneyDto journey) {
        FloraNextMeetingDto meeting = journey != null ? journey.getNextMeeting() : null;
        boolean confirmed = meeting != null
                && FloraScheduleConstants.SCHEDULE_CONFIRMED.equals(meeting.getScheduleStatus())
                && meeting.getTime() != null
                && meeting.getLocationName() != null
                && !meeting.getLocationName().isBlank();
        Long free = journey != null ? journey.getFreeMinutesUntilMeeting() : null;
        boolean canValidate = confirmed && free != null && free >= 0;
        return ScheduleContext.builder()
                .canValidateSchedule(canValidate)
                .freeMinutesUntilMeeting(free)
                .scheduleStatus(meeting != null ? meeting.getScheduleStatus() : null)
                .build();
    }

    public static boolean fitsSchedule(
            ScheduleContext schedule, int roundTripMinutes, int visitMinutes) {
        if (!schedule.isCanValidateSchedule() || schedule.getFreeMinutesUntilMeeting() == null) {
            return false;
        }
        int required = roundTripMinutes + visitMinutes;
        return required <= schedule.getFreeMinutesUntilMeeting();
    }

    public static String resolveFoodMatch(String poiName, String category, UserTravelPreference pref, boolean personalize) {
        if (!personalize || pref == null) return FloraRecommendationConstants.FOOD_UNKNOWN;
        String name = poiName != null ? poiName.toLowerCase(Locale.ROOT) : "";
        Set<String> dislikes = csvLower(pref.getFoodDislikes());
        Set<String> allergies = csvLower(pref.getFoodAllergies());
        for (String term : dislikes) {
            if (!term.isBlank() && name.contains(term)) return FloraRecommendationConstants.FOOD_EXCLUDED;
        }
        for (String term : allergies) {
            if (!term.isBlank() && name.contains(term)) return FloraRecommendationConstants.FOOD_EXCLUDED;
        }
        if ("RESTAURANT".equalsIgnoreCase(category) || "CAFE".equalsIgnoreCase(category)) {
            Set<String> favorites = csvLower(pref.getFavoriteFoods());
            for (String fav : favorites) {
                if (!fav.isBlank() && name.contains(fav)) return FloraRecommendationConstants.FOOD_MATCH;
            }
        }
        return FloraRecommendationConstants.FOOD_UNKNOWN;
    }

    public static String resolveBudgetMatch(UserTravelPreference pref, boolean personalize) {
        return FloraRecommendationConstants.BUDGET_UNKNOWN;
    }

    public static int estimateRoundTripMinutes(int distanceMeters, int walkingSpeedMpm) {
        if (walkingSpeedMpm <= 0) walkingSpeedMpm = 60;
        return Math.max(2, (int) Math.ceil((2.0 * distanceMeters) / walkingSpeedMpm));
    }

    public static int visitMinutesForCategory(String category, FloraRecommendationProperties props) {
        String key = category != null ? category.toLowerCase(Locale.ROOT).replace('_', '-') : "attraction";
        return props.getDefaultVisitMinutes().getOrDefault(key, 30);
    }

    public static String mapOsmCategory(java.util.Map<String, Object> tags) {
        if (tags == null) return "ATTRACTION";
        Object amenity = tags.get("amenity");
        if (amenity != null) {
            String a = amenity.toString();
            if (a.contains("cafe")) return "CAFE";
            if (a.contains("toilets")) return "RESTROOM";
            if (a.contains("restaurant") || a.contains("fast_food")) return "RESTAURANT";
        }
        Object tourism = tags.get("tourism");
        if (tourism != null) {
            String t = tourism.toString();
            if (t.contains("viewpoint")) return "PHOTO_SPOT";
            return "ATTRACTION";
        }
        if (tags.containsKey("shop")) return "SHOPPING";
        return "ATTRACTION";
    }

    public static List<String> baseWarnings(boolean includeTravel, boolean includeFood) {
        List<String> w = new ArrayList<>();
        if (includeTravel) w.add(FloraRecommendationConstants.TRAVEL_ESTIMATE_WARNING);
        if (includeFood) w.add(FloraRecommendationConstants.FOOD_ALLERGY_WARNING);
        return w;
    }

    private static Set<String> csvLower(String csv) {
        if (csv == null || csv.isBlank()) return Set.of();
        return java.util.Arrays.stream(csv.split(","))
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }
}
