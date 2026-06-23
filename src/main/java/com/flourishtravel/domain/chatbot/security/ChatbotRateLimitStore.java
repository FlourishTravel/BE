package com.flourishtravel.domain.chatbot.security;

/**
 * Pluggable store for chatbot rate-limit counters.
 * Replace with a Redis-backed implementation when Redis is available in all deployments.
 */
public interface ChatbotRateLimitStore {

    ChatbotRateLimitDecision tryConsume(String clientKey, int requestsPerMinute, int requestsPerHour);

    void cleanupExpired();
}
