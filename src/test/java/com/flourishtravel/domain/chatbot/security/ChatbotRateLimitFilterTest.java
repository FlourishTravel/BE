package com.flourishtravel.domain.chatbot.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.domain.chatbot.security.ChatbotRateLimitProperties.LimitTier;
import com.flourishtravel.domain.chatbot.security.ChatbotRateLimitProperties.TrustedProxy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class ChatbotRateLimitFilterTest {

    private ChatbotRateLimitFilter filter;
    private InMemoryChatbotRateLimitStore store;
    private ChatbotRateLimitProperties properties;

    @BeforeEach
    void setUp() {
        properties = new ChatbotRateLimitProperties();
        properties.setAnonymous(new LimitTier(1, 100));
        properties.setAuthenticated(new LimitTier(10, 100));
        properties.setTrustedProxy(new TrustedProxy());

        store = new InMemoryChatbotRateLimitStore(properties);
        ChatbotClientKeyResolver keyResolver = new ChatbotClientKeyResolver(properties);
        ChatbotSecurityAuditLogger auditLogger = new ChatbotSecurityAuditLogger();
        ChatbotRateLimitService rateLimitService =
                new ChatbotRateLimitService(properties, store, keyResolver, auditLogger);
        filter = new ChatbotRateLimitFilter(rateLimitService, new ObjectMapper());
    }

    @Test
    void rateLimitedResponse_usesApiResponseStructureAndRetryAfter() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/chatbot/message");
        request.setServletPath("/chatbot/message");
        request.setRemoteAddr("198.51.100.7");

        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(request, firstResponse, new MockFilterChain());
        assertEquals(200, firstResponse.getStatus());

        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        blockedResponse.setCharacterEncoding("UTF-8");
        filter.doFilter(request, blockedResponse, new MockFilterChain());

        assertEquals(429, blockedResponse.getStatus());
        assertNotNull(blockedResponse.getHeader("Retry-After"));
        assertTrue(Integer.parseInt(blockedResponse.getHeader("Retry-After")) > 0);

        ApiResponse<?> body = new ObjectMapper().readValue(blockedResponse.getContentAsString(), ApiResponse.class);
        assertFalse(body.isSuccess());
        assertEquals(ChatbotRateLimitService.RATE_LIMIT_MESSAGE, body.getMessage());
    }
}
