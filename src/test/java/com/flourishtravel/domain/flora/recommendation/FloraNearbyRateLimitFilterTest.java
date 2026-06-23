package com.flourishtravel.domain.flora.recommendation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.domain.chatbot.security.ChatbotRateLimitProperties;
import com.flourishtravel.domain.chatbot.security.ChatbotSecurityAuditLogger;
import com.flourishtravel.domain.chatbot.security.InMemoryChatbotRateLimitStore;
import com.flourishtravel.domain.user.entity.User;
import com.flourishtravel.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FloraNearbyRateLimitFilterTest {

    private FloraNearbyRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        FloraRecommendationProperties properties = new FloraRecommendationProperties();
        properties.getRateLimit().setRequestsPerMinute(1);

        ChatbotRateLimitProperties chatbotProps = new ChatbotRateLimitProperties();
        InMemoryChatbotRateLimitStore store = new InMemoryChatbotRateLimitStore(chatbotProps);
        FloraNearbyRateLimitService rateLimitService =
                new FloraNearbyRateLimitService(store, properties, new ChatbotSecurityAuditLogger());
        filter = new FloraNearbyRateLimitFilter(rateLimitService, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rateLimitReturns429WithApiResponse() throws Exception {
        User user = User.builder().email("u@test.com").fullName("Test").passwordHash("x").isActive(true).build();
        user.setId(UUID.randomUUID());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new UserPrincipal(user), null));

        String path = "/flora/bookings/" + UUID.randomUUID() + "/nearby-recommendations";
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setServletPath(path);

        MockHttpServletResponse first = new MockHttpServletResponse();
        filter.doFilter(request, first, new MockFilterChain());
        assertEquals(200, first.getStatus());

        MockHttpServletResponse second = new MockHttpServletResponse();
        second.setCharacterEncoding("UTF-8");
        filter.doFilter(request, second, new MockFilterChain());

        assertEquals(429, second.getStatus());
        ApiResponse<?> body = new ObjectMapper().readValue(second.getContentAsString(), ApiResponse.class);
        assertFalse(body.isSuccess());
    }
}
