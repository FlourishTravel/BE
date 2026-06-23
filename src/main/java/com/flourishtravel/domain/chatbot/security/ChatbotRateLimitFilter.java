package com.flourishtravel.domain.chatbot.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.security.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ChatbotRateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> RATE_LIMITED_PATHS = Set.of(
            "/chatbot/message",
            "/chatbot/nearby-places",
            "/chatbot/weather-forecast"
    );

    private final ChatbotRateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        if (!RATE_LIMITED_PATHS.contains(path)) {
            return true;
        }
        if ("/chatbot/message".equals(path) && !"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        return !"GET".equalsIgnoreCase(request.getMethod()) && !"POST".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        UUID userId = resolveAuthenticatedUserId();
        String endpoint = request.getServletPath();
        ChatbotRateLimitDecision decision = rateLimitService.check(request, userId, endpoint);
        if (!decision.isAllowed()) {
            response.setStatus(429);
            if (decision.getRetryAfterSeconds() > 0) {
                response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(decision.getRetryAfterSeconds()));
            }
            response.setCharacterEncoding("UTF-8");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
            objectMapper.writeValue(response.getWriter(),
                    ApiResponse.error(ChatbotRateLimitService.RATE_LIMIT_MESSAGE));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static UUID resolveAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getId();
        }
        return null;
    }
}
