package com.flourishtravel.domain.chatbot.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

class ChatbotClientKeyResolverTest {

    @Test
    void doesNotTrustForwardedHeadersWhenProxyDisabled() {
        ChatbotRateLimitProperties properties = new ChatbotRateLimitProperties();
        properties.getTrustedProxy().setEnabled(false);
        ChatbotClientKeyResolver resolver = new ChatbotClientKeyResolver(properties);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.50");
        request.addHeader("X-Forwarded-For", "1.2.3.4");

        assertEquals("chatbot:anon:203.0.113.50", resolver.resolveClientKey(request, null));
    }

    @Test
    void trustsForwardedHeadersWhenProxyEnabled() {
        ChatbotRateLimitProperties properties = new ChatbotRateLimitProperties();
        properties.getTrustedProxy().setEnabled(true);
        ChatbotClientKeyResolver resolver = new ChatbotClientKeyResolver(properties);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.99, 10.0.0.1");

        assertEquals("chatbot:anon:203.0.113.99", resolver.resolveClientKey(request, null));
    }
}
