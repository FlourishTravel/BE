package com.flourishtravel.domain.flora.recommendation;

import com.flourishtravel.domain.chatbot.client.OverpassClient;
import com.flourishtravel.domain.chatbot.dto.ChatbotRequest;
import com.flourishtravel.domain.chatbot.security.ChatbotSecurityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FloraGiftShoppingHintServiceTest {

    @Mock OverpassClient overpassClient;
    @Mock ChatbotSecurityService chatbotSecurityService;
    @InjectMocks FloraGiftShoppingHintService service;

    @Test
    void namedVenueSkipsOverpass() {
        ChatbotRequest request = new ChatbotRequest();
        request.setContent("Đang ở Big C Ratchadamri, mua quà cho mẹ 500 baht");
        request.setLatitude(13.74);
        request.setLongitude(100.54);
        UUID userId = UUID.randomUUID();

        String hint = service.buildHint(request, userId);
        assertTrue(hint.contains("Big C"));
        assertTrue(hint.contains("500"));
        verify(overpassClient, never()).findNearbyPois(anyDouble(), anyDouble(), anyInt(), any(), anyInt());
    }

    @Test
    void gpsFillsVenueWhenUnnamed() {
        ChatbotRequest request = new ChatbotRequest();
        request.setContent("Mua quà cho mẹ 500 baht");
        request.setLatitude(13.74);
        request.setLongitude(100.54);
        UUID userId = UUID.randomUUID();
        when(chatbotSecurityService.shouldIncludeLocationInHint(request, userId)).thenReturn(true);
        when(overpassClient.findNearbyPois(anyDouble(), anyDouble(), anyInt(), any(), anyInt()))
                .thenReturn(List.of(Map.of(
                        "lat", 13.7401,
                        "lon", 100.5401,
                        "tags", Map.of("name", "Big C Supercenter", "shop", "supermarket")
                )));

        String hint = service.buildHint(request, userId);
        assertTrue(hint.contains("Big C Supercenter"));
    }
}
