package com.flourishtravel.domain.chatbot.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ChatbotClientKeyResolver {

    private final ChatbotRateLimitProperties properties;

    public String resolveClientKey(HttpServletRequest request, UUID authenticatedUserId) {
        if (authenticatedUserId != null) {
            return "chatbot:user:" + authenticatedUserId;
        }
        return "chatbot:anon:" + normalizeIp(resolveClientIp(request));
    }

    public String resolveClientIp(HttpServletRequest request) {
        if (properties.getTrustedProxy().isEnabled()) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (StringUtils.hasText(forwarded)) {
                String first = forwarded.split(",")[0].trim();
                if (!first.isEmpty()) {
                    return first;
                }
            }
            String realIp = request.getHeader("X-Real-IP");
            if (StringUtils.hasText(realIp)) {
                return realIp.trim();
            }
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }

    static String normalizeIp(String raw) {
        if (raw == null || raw.isBlank()) {
            return "unknown";
        }
        String ip = raw.trim();
        if (ip.startsWith("[") && ip.contains("]")) {
            ip = ip.substring(1, ip.indexOf(']'));
        }
        if ("::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
            return "127.0.0.1";
        }
        try {
            return InetAddress.getByName(ip).getHostAddress();
        } catch (Exception ignored) {
            return ip.toLowerCase();
        }
    }
}
