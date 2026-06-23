package com.flourishtravel.domain.chatbot.security;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-instance fixed-window rate limiter with bounded memory and automatic expiry.
 * Not suitable for multi-instance production without sticky sessions or Redis replacement.
 */
@Component
@RequiredArgsConstructor
public class InMemoryChatbotRateLimitStore implements ChatbotRateLimitStore {

    private final ChatbotRateLimitProperties properties;
    private final ConcurrentHashMap<String, WindowCounters> counters = new ConcurrentHashMap<>();

    @Override
    public ChatbotRateLimitDecision tryConsume(String clientKey, int requestsPerMinute, int requestsPerHour) {
        long nowMs = System.currentTimeMillis();
        long epochMinute = nowMs / 60_000L;
        long epochHour = nowMs / 3_600_000L;

        WindowCounters entry = counters.compute(clientKey, (key, existing) -> {
            WindowCounters counters = existing != null ? existing : new WindowCounters();
            counters.touch(nowMs);
            if (counters.minuteWindow != epochMinute) {
                counters.minuteWindow = epochMinute;
                counters.minuteCount = 0;
            }
            if (counters.hourWindow != epochHour) {
                counters.hourWindow = epochHour;
                counters.hourCount = 0;
            }
            counters.minuteCount++;
            counters.hourCount++;
            return counters;
        });

        evictIfNeeded(nowMs);

        if (entry.minuteCount > requestsPerMinute) {
            return ChatbotRateLimitDecision.builder()
                    .allowed(false)
                    .retryAfterSeconds(secondsUntilNextMinute(nowMs))
                    .window("minute")
                    .build();
        }
        if (entry.hourCount > requestsPerHour) {
            return ChatbotRateLimitDecision.builder()
                    .allowed(false)
                    .retryAfterSeconds(secondsUntilNextHour(nowMs))
                    .window("hour")
                    .build();
        }
        return ChatbotRateLimitDecision.builder()
                .allowed(true)
                .retryAfterSeconds(0)
                .window("ok")
                .build();
    }

    @Override
    @Scheduled(fixedDelayString = "${app.chatbot.rate-limit.in-memory.cleanup-interval-ms:300000}")
    public void cleanupExpired() {
        long nowMs = System.currentTimeMillis();
        long epochMinute = nowMs / 60_000L;
        long epochHour = nowMs / 3_600_000L;
        long staleAfterMs = Math.max(properties.getInMemory().getCleanupIntervalMs(), 3_600_000L);

        Iterator<Map.Entry<String, WindowCounters>> it = counters.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, WindowCounters> e = it.next();
            WindowCounters value = e.getValue();
            boolean expiredWindows = value.minuteWindow < epochMinute - 1 && value.hourWindow < epochHour - 1;
            boolean stale = nowMs - value.lastAccessMs > staleAfterMs;
            if (expiredWindows && stale) {
                it.remove();
            }
        }
    }

    private void evictIfNeeded(long nowMs) {
        int maxKeys = properties.getInMemory().getMaxKeys();
        if (counters.size() <= maxKeys) {
            return;
        }
        String oldestKey = null;
        long oldestAccess = Long.MAX_VALUE;
        for (Map.Entry<String, WindowCounters> e : counters.entrySet()) {
            if (e.getValue().lastAccessMs < oldestAccess) {
                oldestAccess = e.getValue().lastAccessMs;
                oldestKey = e.getKey();
            }
        }
        if (oldestKey != null) {
            counters.remove(oldestKey);
        }
        cleanupExpired();
    }

    private static int secondsUntilNextMinute(long nowMs) {
        long remainder = 60_000L - (nowMs % 60_000L);
        return (int) Math.max(1, (remainder + 999) / 1000);
    }

    private static int secondsUntilNextHour(long nowMs) {
        long remainder = 3_600_000L - (nowMs % 3_600_000L);
        return (int) Math.max(1, (remainder + 999) / 1000);
    }

    static final class WindowCounters {
        long minuteWindow;
        int minuteCount;
        long hourWindow;
        int hourCount;
        long lastAccessMs;

        void touch(long nowMs) {
            lastAccessMs = nowMs;
        }
    }
}
