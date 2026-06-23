package com.flourishtravel.domain.flora.recommendation;

import com.flourishtravel.domain.chatbot.security.ChatbotRateLimitDecision;
import com.flourishtravel.domain.chatbot.security.ChatbotRateLimitStore;
import com.flourishtravel.domain.chatbot.security.ChatbotSecurityAuditLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FloraNearbyRateLimitService {

    private final ChatbotRateLimitStore rateLimitStore;
    private final FloraRecommendationProperties properties;
    private final ChatbotSecurityAuditLogger auditLogger;

    public ChatbotRateLimitDecision check(UUID userId) {
        String key = "flora-nearby:user:" + userId;
        int perMinute = properties.getRateLimit().getRequestsPerMinute();
        ChatbotRateLimitDecision decision = rateLimitStore.tryConsume(key, perMinute, perMinute * 60);
        if (decision.isAllowed()) {
            auditLogger.rateLimitAllowed("/flora/nearby-recommendations", true, key);
        } else {
            auditLogger.rateLimitBlocked("/flora/nearby-recommendations", true, key, decision.getWindow());
        }
        return decision;
    }
}
