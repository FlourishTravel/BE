package com.flourishtravel.domain.chatbot.security;

import com.flourishtravel.domain.chatbot.dto.ChatbotRequest;
import com.flourishtravel.domain.chatbot.dto.ChatbotResponse;
import com.flourishtravel.domain.chatbot.service.ChatbotService;
import com.flourishtravel.domain.flora.recommendation.FloraNearbyChatbotHelper;
import com.flourishtravel.domain.flora.service.FloraContextBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatbotControllerSecurityTest {

    @Mock ChatbotService chatbotService;
    @Mock FloraContextBuilder floraContextBuilder;
    @Mock ChatbotSecurityService chatbotSecurityService;
    @Mock ChatbotSecurityAuditLogger auditLogger;
    @Mock FloraNearbyChatbotHelper nearbyChatbotHelper;
    @InjectMocks com.flourishtravel.domain.chatbot.controller.ChatbotController controller;

    private void stubNearbyPassthrough() {
        when(nearbyChatbotHelper.maybeEnhance(any(), any(), any()))
                .thenAnswer(inv -> inv.getArgument(1));
    }

    @Test
    void legacyContentPayload_stillWorks() {
        ChatbotRequest request = new ChatbotRequest();
        request.setContent("Tour biển 3 ngày");
        ChatbotResponse botResponse = ChatbotResponse.builder().reply("ok").build();

        when(chatbotSecurityService.bookingAccessResponse(request, null)).thenReturn(java.util.Optional.empty());
        when(chatbotSecurityService.prepareRequest(request, null)).thenReturn(request);
        when(chatbotService.processMessage(request, null)).thenReturn(botResponse);
        when(floraContextBuilder.enrich(botResponse, request, null)).thenReturn(botResponse);
        stubNearbyPassthrough();

        var entity = controller.message(request, null);
        assertTrue(entity.getBody().isSuccess());
        assertEquals("ok", entity.getBody().getData().getReply());
    }

    @Test
    void messageAlias_stillWorks() {
        ChatbotRequest request = new ChatbotRequest();
        request.setMessage("Chính sách hủy tour");
        ChatbotResponse botResponse = ChatbotResponse.builder().reply("policy").build();

        when(chatbotSecurityService.bookingAccessResponse(request, null)).thenReturn(java.util.Optional.empty());
        when(chatbotSecurityService.prepareRequest(request, null)).thenReturn(request);
        when(chatbotService.processMessage(request, null)).thenReturn(botResponse);
        when(floraContextBuilder.enrich(botResponse, request, null)).thenReturn(botResponse);
        stubNearbyPassthrough();

        var entity = controller.message(request, null);
        assertEquals("policy", entity.getBody().getData().getReply());
    }

    @Test
    void requestBodyUserId_doesNotOverrideJwtIdentity() {
        UUID jwtUser = UUID.randomUUID();
        UUID spoofedUser = UUID.randomUUID();
        ChatbotRequest request = new ChatbotRequest();
        request.setContent("hello");
        request.setUserId(spoofedUser.toString());

        com.flourishtravel.security.UserPrincipal principal = mock(com.flourishtravel.security.UserPrincipal.class);
        when(principal.getId()).thenReturn(jwtUser);

        when(chatbotSecurityService.bookingAccessResponse(request, jwtUser)).thenReturn(java.util.Optional.empty());
        when(chatbotSecurityService.prepareRequest(request, jwtUser)).thenReturn(request);
        when(chatbotService.processMessage(request, jwtUser))
                .thenReturn(ChatbotResponse.builder().reply("hi").build());
        when(floraContextBuilder.enrich(any(), eq(request), eq(jwtUser)))
                .thenAnswer(inv -> inv.getArgument(0));
        stubNearbyPassthrough();

        controller.message(request, principal);

        ArgumentCaptor<UUID> userCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(chatbotService).processMessage(any(), userCaptor.capture());
        assertEquals(jwtUser, userCaptor.getValue());
        assertNotEquals(spoofedUser, userCaptor.getValue());
    }
}
