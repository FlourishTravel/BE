package com.flourishtravel.domain.chat;

import com.flourishtravel.common.exception.BadRequestException;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Bộ icon thả cảm xúc trong chat đoàn (kiểu Zalo).
 */
public final class ChatReactionTypes {

    public static final List<String> EMOJIS = List.of("👍", "❤️", "😂", "😮", "😢", "😡");

    private static final Map<String, String> ALIASES = Map.of(
            "like", "👍",
            "love", "❤️",
            "haha", "😂",
            "wow", "😮",
            "sad", "😢",
            "angry", "😡"
    );

    private ChatReactionTypes() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException("Chọn một icon cảm xúc.");
        }
        String trimmed = raw.trim();
        if (EMOJIS.contains(trimmed)) {
            return trimmed;
        }
        String alias = ALIASES.get(trimmed.toLowerCase(Locale.ROOT));
        if (alias != null) {
            return alias;
        }
        throw new BadRequestException("Icon cảm xúc không được hỗ trợ.");
    }
}
