package com.flourishtravel.domain.flora.feedback;

import com.flourishtravel.domain.flora.dto.TravelPreferencesDto;
import com.flourishtravel.domain.flora.dto.UpdateTravelPreferencesRequest;
import com.flourishtravel.domain.flora.feedback.FloraFeedbackTagCatalog.SuggestionKind;
import com.flourishtravel.domain.flora.feedback.FloraFeedbackTagCatalog.TagDefinition;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * Builds a partial PATCH body from explicitly selected feedback chips — never persists by itself.
 */
public final class FloraPreferenceSuggestionMerger {

    private FloraPreferenceSuggestionMerger() {}

    public static UpdateTravelPreferencesRequest buildPatchRequest(List<String> selectedTagIds,
                                                                   TravelPreferencesDto current) {
        TravelPreferencesDto base = current != null ? current : emptyCurrent();
        TravelPreferencesDto merged = mergePreview(selectedTagIds, base);
        return toPatchRequest(base, merged);
    }

    public static TravelPreferencesDto mergePreview(List<String> selectedTagIds, TravelPreferencesDto current) {
        TravelPreferencesDto result = copyOf(current != null ? current : emptyCurrent());
        List<TagDefinition> tags = FloraFeedbackTagCatalog.resolveKnown(selectedTagIds);
        for (TagDefinition tag : tags) {
            if (tag.getSuggestedPreferenceField() == null || tag.getSuggestedValue() == null) continue;
            applyTag(result, tag);
        }
        return result;
    }

    public static List<PreferenceChange> describeChanges(List<String> selectedTagIds,
                                                         TravelPreferencesDto current) {
        TravelPreferencesDto base = current != null ? current : emptyCurrent();
        TravelPreferencesDto merged = mergePreview(selectedTagIds, base);
        List<PreferenceChange> changes = new ArrayList<>();
        appendListChanges(changes, "travelStyles", base.getTravelStyles(), merged.getTravelStyles());
        appendListChanges(changes, "preferredActivities", base.getPreferredActivities(), merged.getPreferredActivities());
        appendListChanges(changes, "avoidedActivities", base.getAvoidedActivities(), merged.getAvoidedActivities());
        if (!Objects.equals(base.getTravelPace(), merged.getTravelPace()) && merged.getTravelPace() != null) {
            changes.add(new PreferenceChange("travelPace", base.getTravelPace(), merged.getTravelPace()));
        }
        return changes;
    }

    private static void applyTag(TravelPreferencesDto dto, TagDefinition tag) {
        if (tag.getSuggestionKind() == SuggestionKind.SET_FIELD) {
            if ("travelPace".equals(tag.getSuggestedPreferenceField())) {
                dto.setTravelPace(tag.getSuggestedValue());
            }
            return;
        }
        if ("travelStyles".equals(tag.getSuggestedPreferenceField())) {
            dto.setTravelStyles(appendUnique(dto.getTravelStyles(), tag.getSuggestedValue()));
        } else if ("preferredActivities".equals(tag.getSuggestedPreferenceField())) {
            dto.setPreferredActivities(appendUnique(dto.getPreferredActivities(), tag.getSuggestedValue()));
        } else if ("avoidedActivities".equals(tag.getSuggestedPreferenceField())) {
            dto.setAvoidedActivities(appendUnique(dto.getAvoidedActivities(), tag.getSuggestedValue()));
        }
    }

    private static List<String> appendUnique(List<String> existing, String value) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        if (existing != null) {
            existing.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty()).forEach(set::add);
        }
        if (value != null && !value.isBlank()) set.add(value.trim());
        return new ArrayList<>(set);
    }

    private static void appendListChanges(List<PreferenceChange> changes, String field,
                                          List<String> before, List<String> after) {
        List<String> b = before != null ? before : List.of();
        List<String> a = after != null ? after : List.of();
        for (String item : a) {
            if (item != null && !item.isBlank() && b.stream().noneMatch(x -> x.equalsIgnoreCase(item))) {
                changes.add(new PreferenceChange(field, null, item));
            }
        }
    }

    private static UpdateTravelPreferencesRequest toPatchRequest(TravelPreferencesDto before,
                                                                 TravelPreferencesDto after) {
        UpdateTravelPreferencesRequest req = new UpdateTravelPreferencesRequest();
        if (!listEquals(before.getTravelStyles(), after.getTravelStyles())) {
            req.setTravelStyles(after.getTravelStyles());
        }
        if (!listEquals(before.getPreferredActivities(), after.getPreferredActivities())) {
            req.setPreferredActivities(after.getPreferredActivities());
        }
        if (!listEquals(before.getAvoidedActivities(), after.getAvoidedActivities())) {
            req.setAvoidedActivities(after.getAvoidedActivities());
        }
        if (!Objects.equals(before.getTravelPace(), after.getTravelPace()) && after.getTravelPace() != null) {
            req.setTravelPace(after.getTravelPace());
        }
        return req;
    }

    private static boolean listEquals(List<String> a, List<String> b) {
        List<String> left = a != null ? a : List.of();
        List<String> right = b != null ? b : List.of();
        if (left.size() != right.size()) return false;
        for (int i = 0; i < left.size(); i++) {
            if (!Objects.equals(left.get(i), right.get(i))) return false;
        }
        return true;
    }

    private static TravelPreferencesDto emptyCurrent() {
        return TravelPreferencesDto.builder()
                .travelStyles(List.of())
                .preferredActivities(List.of())
                .avoidedActivities(List.of())
                .build();
    }

    private static TravelPreferencesDto copyOf(TravelPreferencesDto src) {
        return TravelPreferencesDto.builder()
                .travelStyles(copyList(src.getTravelStyles()))
                .budgetLevel(src.getBudgetLevel())
                .favoriteDestinations(copyList(src.getFavoriteDestinations()))
                .favoriteFoods(copyList(src.getFavoriteFoods()))
                .foodDislikes(copyList(src.getFoodDislikes()))
                .foodAllergies(copyList(src.getFoodAllergies()))
                .preferredActivities(copyList(src.getPreferredActivities()))
                .avoidedActivities(copyList(src.getAvoidedActivities()))
                .travelPace(src.getTravelPace())
                .travelingWithChildren(src.getTravelingWithChildren())
                .travelingWithElderly(src.getTravelingWithElderly())
                .notificationConsent(src.getNotificationConsent())
                .locationConsent(src.getLocationConsent())
                .personalizationConsent(src.getPersonalizationConsent())
                .build();
    }

    private static List<String> copyList(List<String> list) {
        return list != null ? new ArrayList<>(list) : new ArrayList<>();
    }

    public record PreferenceChange(String field, String before, String after) {}
}
