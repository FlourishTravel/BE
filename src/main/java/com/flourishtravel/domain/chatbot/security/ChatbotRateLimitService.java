package com.flourishtravel.domain.chatbot.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatbotRateLimitService {

    public static final String RATE_LIMIT_MESSAGE =
            "Flora đang nhận khá nhiều yêu cầu. Bạn vui lòng thử lại sau ít phút nhé.";

    private final ChatbotRateLimitProperties properties;
    private final ChatbotRateLimitStore rateLimitStore;
    private final ChatbotClientKeyResolver clientKeyResolver;
    private final ChatbotSecurityAuditLogger auditLogger;

    public ChatbotRateLimitDecision check(HttpServletRequest request, UUID authenticatedUserId, String endpoint) {
        String clientKey = clientKeyResolver.resolveClientKey(request, authenticatedUserId);
        ChatbotRateLimitProperties.LimitTier tier = authenticatedUserId != null
                ? properties.getAuthenticated()
                : properties.getAnonymous();

        ChatbotRateLimitDecision decision = rateLimitStore.tryConsume(
                clientKey,
                tier.getRequestsPerMinute(),
                tier.getRequestsPerHour());

        if (decision.isAllowed()) {
            auditLogger.rateLimitAllowed(endpoint, authenticatedUserId != null, clientKey);
        } else {
            auditLogger.rateLimitBlocked(endpoint, authenticatedUserId != null, clientKey, decision.getWindow());
        }
        return decision;
    }
}
