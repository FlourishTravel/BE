package com.flourishtravel.domain.flora.feedback;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Chips for HDV feedback on a completed booking. Not mapped to travel preferences.
 */
public final class FloraGuideFeedbackTagCatalog {

    public static final int MAX_TAGS_PER_REVIEW = 10;

    public record TagDefinition(String id, String label, String category) {}

    private static final List<TagDefinition> ALL = List.of(
            new TagDefinition("GUIDE_ATTENTIVE", "HDV tận tâm", "LIKED"),
            new TagDefinition("GUIDE_PROFESSIONAL", "HDV chuyên môn tốt", "LIKED"),
            new TagDefinition("GUIDE_FRIENDLY", "HDV thân thiện", "LIKED"),
            new TagDefinition("GUIDE_KNOWLEDGE", "HDV hiểu biết địa phương", "LIKED"),
            new TagDefinition("GUIDE_ATTITUDE", "Thái độ HDV chưa tốt", "IMPROVE"),
            new TagDefinition("GUIDE_COMMUNICATION", "HDV giao tiếp chưa rõ", "IMPROVE"),
            new TagDefinition("GUIDE_PACE", "HDV dẫn đoàn chưa hợp nhịp", "IMPROVE")
    );

    private static final Map<String, TagDefinition> BY_ID = ALL.stream()
            .collect(Collectors.toUnmodifiableMap(TagDefinition::id, t -> t));

    private FloraGuideFeedbackTagCatalog() {}

    public static List<TagDefinition> all() {
        return ALL;
    }

    public static Optional<TagDefinition> find(String id) {
        if (id == null || id.isBlank()) return Optional.empty();
        return Optional.ofNullable(BY_ID.get(id.trim()));
    }

    public static void validateTagIds(List<String> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) return;
        if (tagIds.size() > MAX_TAGS_PER_REVIEW) {
            throw new IllegalArgumentException("Tối đa " + MAX_TAGS_PER_REVIEW + " nhãn phản hồi HDV");
        }
        for (String id : tagIds) {
            if (find(id).isEmpty()) {
                throw new IllegalArgumentException("Nhãn phản hồi HDV không hợp lệ: " + id);
            }
        }
    }

    public static List<TagDefinition> resolveKnown(List<String> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) return Collections.emptyList();
        return tagIds.stream()
                .map(FloraGuideFeedbackTagCatalog::find)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .distinct()
                .toList();
    }
}
