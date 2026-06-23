package com.flourishtravel.domain.chatbot.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.chatbot.rate-limit")
public class ChatbotRateLimitProperties {

    private LimitTier authenticated = new LimitTier(30, 300);
    private LimitTier anonymous = new LimitTier(10, 80);
    private InMemory inMemory = new InMemory();
    private TrustedProxy trustedProxy = new TrustedProxy();

    @Getter
    @Setter
    public static class LimitTier {
        private int requestsPerMinute = 10;
        private int requestsPerHour = 80;

        public LimitTier() {
        }

        public LimitTier(int requestsPerMinute, int requestsPerHour) {
            this.requestsPerMinute = requestsPerMinute;
            this.requestsPerHour = requestsPerHour;
        }
    }

    @Getter
    @Setter
    public static class InMemory {
        private int maxKeys = 10_000;
        private long cleanupIntervalMs = 300_000L;
    }

    @Getter
    @Setter
    public static class TrustedProxy {
        private boolean enabled = false;
    }
}
