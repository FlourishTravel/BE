package com.flourishtravel.domain.chat;

import com.flourishtravel.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChatReactionTypesTest {

    @Test
    void keepsSupportedEmoji() {
        assertEquals("👍", ChatReactionTypes.normalize("👍"));
        assertEquals("❤️", ChatReactionTypes.normalize("❤️"));
    }

    @Test
    void mapsAliases() {
        assertEquals("👍", ChatReactionTypes.normalize("like"));
        assertEquals("😂", ChatReactionTypes.normalize("HAHA"));
        assertEquals("😡", ChatReactionTypes.normalize("angry"));
    }

    @Test
    void rejectsUnknown() {
        assertThrows(BadRequestException.class, () -> ChatReactionTypes.normalize("🔥"));
        assertThrows(BadRequestException.class, () -> ChatReactionTypes.normalize(" "));
        assertThrows(BadRequestException.class, () -> ChatReactionTypes.normalize(null));
    }
}
