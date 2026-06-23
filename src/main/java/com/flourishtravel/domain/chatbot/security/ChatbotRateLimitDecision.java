package com.flourishtravel.domain.chatbot.security;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ChatbotRateLimitDecision {
    boolean allowed;
    int retryAfterSeconds;
    String window;
}
