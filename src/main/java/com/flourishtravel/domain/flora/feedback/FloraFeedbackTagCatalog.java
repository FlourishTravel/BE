package com.flourishtravel.domain.flora.feedback;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Deterministic catalog of post-tour feedback chips and their optional preference mappings.
 */
public final class FloraFeedbackTagCatalog {

    public static final int MAX_TAGS_PER_REVIEW = 10;

    @Getter
    @RequiredArgsConstructor
    public enum TagCategory {
        LIKED("LIKED"),
        IMPROVE("IMPROVE");

        private final String value;
    }

    @Getter
    @RequiredArgsConstructor
    public enum SuggestionKind {
        APPEND_LIST,
        SET_FIELD
    }

    @Getter
    @RequiredArgsConstructor
    public static final class TagDefinition {
        private final String id;
        private final String label;
        private final TagCategory category;
        private final String suggestedPreferenceField;
        private final String suggestedValue;
        private final SuggestionKind suggestionKind;
    }

    private static final List<TagDefinition> ALL = List.of(
            tag("PHOTO_SPOTS", "Thích địa điểm chụp ảnh", TagCategory.LIKED,
                    "preferredActivities", "chụp ảnh", SuggestionKind.APPEND_LIST),
            tag("COFFEE", "Thích quán cà phê", TagCategory.LIKED,
                    "preferredActivities", "cà phê", SuggestionKind.APPEND_LIST),
            tag("LOCAL_CUISINE", "Thích ẩm thực địa phương", TagCategory.LIKED,
                    "preferredActivities", "ẩm thực địa phương", SuggestionKind.APPEND_LIST),
            tag("RESORT", "Thích nghỉ dưỡng", TagCategory.LIKED,
                    "travelStyles", "Nghỉ dưỡng", SuggestionKind.APPEND_LIST),
            tag("NATURE", "Thích khám phá thiên nhiên", TagCategory.LIKED,
                    "preferredActivities", "thiên nhiên", SuggestionKind.APPEND_LIST),
            tag("RELAXED_PACE", "Thích lịch trình thư thả", TagCategory.LIKED,
                    "travelPace", "chậm", SuggestionKind.SET_FIELD),
            tag("ACTIVE_PACE", "Thích lịch trình năng động", TagCategory.LIKED,
                    "travelPace", "nhanh", SuggestionKind.SET_FIELD),
            tag("DISLIKE_TOO_MUCH_TRAVEL", "Không thích di chuyển quá nhiều", TagCategory.IMPROVE,
                    "avoidedActivities", "di chuyển nhiều", SuggestionKind.APPEND_LIST),
            tag("DISLIKE_RUSHED", "Không thích lịch trình quá gấp", TagCategory.IMPROVE,
                    "avoidedActivities", "lịch trình gấp", SuggestionKind.APPEND_LIST),
            tag("WANT_FREE_TIME", "Muốn có thêm thời gian tự do", TagCategory.IMPROVE,
                    "preferredActivities", "thời gian tự do", SuggestionKind.APPEND_LIST)
    );

    private static final Map<String, TagDefinition> BY_ID = ALL.stream()
            .collect(Collectors.toUnmodifiableMap(TagDefinition::getId, t -> t));

    private FloraFeedbackTagCatalog() {}

    private static TagDefinition tag(String id, String label, TagCategory category,
                                       String field, String value, SuggestionKind kind) {
        return new TagDefinition(id, label, category, field, value, kind);
    }

    public static List<TagDefinition> all() {
        return ALL;
    }

    public static Optional<TagDefinition> find(String id) {
        if (id == null || id.isBlank()) return Optional.empty();
        return Optional.ofNullable(BY_ID.get(id.trim()));
    }

    public static List<TagDefinition> resolveKnown(List<String> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) return Collections.emptyList();
        return tagIds.stream()
                .map(FloraFeedbackTagCatalog::find)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .distinct()
                .toList();
    }

    public static void validateTagIds(List<String> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) return;
        if (tagIds.size() > MAX_TAGS_PER_REVIEW) {
            throw new IllegalArgumentException("Tối đa " + MAX_TAGS_PER_REVIEW + " nhãn phản hồi");
        }
        for (String id : tagIds) {
            if (find(id).isEmpty()) {
                throw new IllegalArgumentException("Nhãn phản hồi không hợp lệ: " + id);
            }
        }
    }

    public static String joinTagIds(List<String> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) return null;
        return String.join(",", tagIds.stream().map(String::trim).filter(s -> !s.isEmpty()).toList());
    }

    public static List<String> splitTagIds(String csv) {
        if (csv == null || csv.isBlank()) return Collections.emptyList();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
