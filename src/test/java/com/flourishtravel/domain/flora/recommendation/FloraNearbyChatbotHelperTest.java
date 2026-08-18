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
    void doesNotAddNearbyActionForInStoreGift() {
        UUID bookingId = UUID.randomUUID();
        ChatbotRequest request = new ChatbotRequest();
        request.setContent("Đang ở Big C Ratchadamri, mua quà cho mẹ 500 baht");
        request.setBookingId(bookingId);
        ChatbotResponse response = ChatbotResponse.builder().reply("OK").build();

        ChatbotResponse enhanced = helper.maybeEnhance(request, response, UUID.randomUUID());
        assertTrue(enhanced.getSuggestedActions() == null
                || enhanced.getSuggestedActions().stream()
                .noneMatch(a -> FloraRecommendationConstants.ACTION_OPEN_NEARBY.equals(a.getType())));
    }

    @Test
    void addsShoppingNearbyActionForGiftNearHere() {
        UUID bookingId = UUID.randomUUID();
        ChatbotRequest request = new ChatbotRequest();
        request.setContent("Mua quà gần đây cho mẹ 500 baht");
        request.setBookingId(bookingId);
        ChatbotResponse response = ChatbotResponse.builder().reply("OK").build();

        ChatbotResponse enhanced = helper.maybeEnhance(request, response, UUID.randomUUID());
        assertTrue(enhanced.getSuggestedActions().stream()
                .anyMatch(a -> FloraRecommendationConstants.ACTION_OPEN_NEARBY.equals(a.getType())
                        && a.getLabel().toLowerCase().contains("mua sắm")));
    }
}
