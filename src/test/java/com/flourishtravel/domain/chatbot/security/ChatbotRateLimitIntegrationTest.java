package com.flourishtravel.domain.chatbot.security;

import com.flourishtravel.domain.chatbot.security.ChatbotRateLimitProperties.InMemory;
import com.flourishtravel.domain.chatbot.security.ChatbotRateLimitProperties.LimitTier;
import com.flourishtravel.domain.chatbot.security.ChatbotRateLimitProperties.TrustedProxy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ChatbotRateLimitIntegrationTest {

    private InMemoryChatbotRateLimitStore store;
    private ChatbotRateLimitService rateLimitService;
    private ChatbotRateLimitProperties properties;

    @BeforeEach
    void setUp() {
        properties = new ChatbotRateLimitProperties();
        properties.setAnonymous(new LimitTier(3, 100));
        properties.setAuthenticated(new LimitTier(5, 200));
        properties.setInMemory(new InMemory());
        properties.getInMemory().setMaxKeys(1000);
        properties.setTrustedProxy(new TrustedProxy());

        store = new InMemoryChatbotRateLimitStore(properties);
        ChatbotClientKeyResolver keyResolver = new ChatbotClientKeyResolver(properties);
        ChatbotSecurityAuditLogger auditLogger = new ChatbotSecurityAuditLogger();
        rateLimitService = new ChatbotRateLimitService(properties, store, keyResolver, auditLogger);
    }

    @Test
    void anonymousClient_allowedWithinLimit() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/chatbot/message");
        request.setRemoteAddr("203.0.113.10");

        for (int i = 0; i < 3; i++) {
            ChatbotRateLimitDecision decision = rateLimitService.check(request, null, "/chatbot/message");
            assertTrue(decision.isAllowed(), "request " + i);
        }
    }

    @Test
    void anonymousClient_blockedAfterExceedingMinuteLimit() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/chatbot/message");
        request.setRemoteAddr("203.0.113.11");

        for (int i = 0; i < 3; i++) {
            assertTrue(rateLimitService.check(request, null, "/chatbot/message").isAllowed());
        }
        ChatbotRateLimitDecision blocked = rateLimitService.check(request, null, "/chatbot/message");
        assertFalse(blocked.isAllowed());
        assertTrue(blocked.getRetryAfterSeconds() > 0);
    }

    @Test
    void authenticatedClient_usesSeparateKeyFromAnonymous() {
        UUID userId = UUID.randomUUID();
        MockHttpServletRequest anonRequest = new MockHttpServletRequest("POST", "/chatbot/message");
        anonRequest.setRemoteAddr("203.0.113.12");
        MockHttpServletRequest authRequest = new MockHttpServletRequest("POST", "/chatbot/message");
        authRequest.setRemoteAddr("203.0.113.12");

        for (int i = 0; i < 3; i++) {
            assertTrue(rateLimitService.check(anonRequest, null, "/chatbot/message").isAllowed());
        }
        assertFalse(rateLimitService.check(anonRequest, null, "/chatbot/message").isAllowed());
        assertTrue(rateLimitService.check(authRequest, userId, "/chatbot/message").isAllowed());
    }

    @Test
    void twoAuthenticatedUsers_doNotShareQuota() {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/chatbot/message");
        request.setRemoteAddr("203.0.113.13");

        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimitService.check(request, userA, "/chatbot/message").isAllowed());
        }
        assertFalse(rateLimitService.check(request, userA, "/chatbot/message").isAllowed());
        assertTrue(rateLimitService.check(request, userB, "/chatbot/message").isAllowed());
    }
}
