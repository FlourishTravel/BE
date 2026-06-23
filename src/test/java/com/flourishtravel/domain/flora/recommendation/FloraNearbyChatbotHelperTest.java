package com.flourishtravel.domain.flora.recommendation;

import com.flourishtravel.domain.chatbot.dto.ChatbotRequest;
import com.flourishtravel.domain.chatbot.dto.ChatbotResponse;
import com.flourishtravel.domain.flora.FloraRecommendationConstants;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FloraNearbyChatbotHelperTest {

    private final FloraNearbyChatbotHelper helper = new FloraNearbyChatbotHelper();

    @Test
    void addsOpenNearbyActionWhenIntentAndBookingPresent() {
        UUID bookingId = UUID.randomUUID();
        ChatbotRequest request = new ChatbotRequest();
        request.setContent("Gợi ý quán ăn gần đây");
        request.setBookingId(bookingId);
        ChatbotResponse response = ChatbotResponse.builder().reply("OK").build();

        ChatbotResponse enhanced = helper.maybeEnhance(request, response, UUID.randomUUID());
        assertTrue(enhanced.getSuggestedActions().stream()
                .anyMatch(a -> FloraRecommendationConstants.ACTION_OPEN_NEARBY.equals(a.getType())));
    }

    @Test
    void doesNotAddActionWithoutBooking() {
        ChatbotRequest request = new ChatbotRequest();
        request.setContent("Gợi ý quán ăn gần đây");
        ChatbotResponse response = ChatbotResponse.builder().reply("OK").build();

        ChatbotResponse enhanced = helper.maybeEnhance(request, response, UUID.randomUUID());
        assertNull(enhanced.getSuggestedActions());
    }
}
