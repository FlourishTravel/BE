package com.flourishtravel.domain.chat.service;

import com.flourishtravel.domain.chatbot.dto.ChatbotResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TourGroupFloraServiceFormatTest {

    @Test
    void formatReplyIncludesTourTitles() {
        ChatbotResponse response = ChatbotResponse.builder()
                .reply("Flora gợi ý vài tour phù hợp.")
                .tours(List.of(ChatbotResponse.TourCard.builder().title("Bangkok độc bản").build()))
                .build();
        String text = TourGroupFloraService.formatReply(response);
        assertTrue(text.contains("Flora gợi ý"));
        assertTrue(text.contains("Bangkok độc bản"));
    }

    @Test
    void formatReplyEmptyWhenNoContent() {
        assertEquals("", TourGroupFloraService.formatReply(null));
        assertEquals("", TourGroupFloraService.formatReply(ChatbotResponse.builder().build()));
    }
}
