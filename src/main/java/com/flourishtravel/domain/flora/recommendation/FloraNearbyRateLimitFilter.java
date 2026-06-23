package com.flourishtravel.domain.flora.recommendation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.domain.chatbot.security.ChatbotRateLimitDecision;
import com.flourishtravel.domain.chatbot.security.ChatbotRateLimitService;
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
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class FloraNearbyRateLimitFilter extends OncePerRequestFilter {

    private static final Pattern NEARBY_PATH =
            Pattern.compile("^/flora/bookings/[0-9a-fA-F-]{36}/nearby-recommendations$");

    private final FloraNearbyRateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) return true;
        return !NEARBY_PATH.matcher(request.getServletPath()).matches();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        UUID userId = resolveUserId();
        if (userId == null) {
            filterChain.doFilter(request, response);
            return;
        }
        ChatbotRateLimitDecision decision = rateLimitService.check(userId);
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

    private static UUID resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getId();
        }
        return null;
    }
}
