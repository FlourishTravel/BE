package com.flourishtravel.domain.chatbot.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

@Component
@Slf4j
public class ChatbotSecurityAuditLogger {

    public void rateLimitAllowed(String endpoint, boolean authenticated, String clientKey) {
        log.info("event=rate_limit_allowed endpoint={} authenticated_or_anonymous={} client_key_hash={}",
                endpoint, authenticated ? "authenticated" : "anonymous", hashClientKey(clientKey));
    }

    public void rateLimitBlocked(String endpoint, boolean authenticated, String clientKey, String window) {
        log.info("event=rate_limit_blocked endpoint={} authenticated_or_anonymous={} client_key_hash={} window={}",
                endpoint, authenticated ? "authenticated" : "anonymous", hashClientKey(clientKey), window);
    }

    public void chatbotRequestType(String requestType, boolean authenticated) {
        log.info("event=chatbot_request_type type={} authenticated_or_anonymous={}",
                requestType, authenticated ? "authenticated" : "anonymous");
    }

    public void bookingContextRequested(UUID userId) {
        log.info("event=booking_context_requested user_id_hash={}", hashUuid(userId));
    }

    public void bookingContextDenied(boolean authenticated) {
        log.info("event=booking_context_denied authenticated_or_anonymous={}",
                authenticated ? "authenticated" : "anonymous");
    }

    public void llmFallbackUsed(boolean authenticated) {
        log.info("event=llm_fallback_used authenticated_or_anonymous={}",
                authenticated ? "authenticated" : "anonymous");
    }

    static String hashClientKey(String clientKey) {
        return sha256Prefix(clientKey);
    }

    static String hashUuid(UUID id) {
        return id == null ? "none" : sha256Prefix(id.toString());
    }

    private static String sha256Prefix(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 8);
        } catch (Exception e) {
            return "unknown";
        }
    }
}
