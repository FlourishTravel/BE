package com.flourishtravel.domain.flora.service;

import com.flourishtravel.domain.chatbot.dto.ChatbotRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FloraContextBuilderTest {

    @Test
    void resolveContent_prefersContentOverMessage() {
        ChatbotRequest req = new ChatbotRequest();
        req.setContent("hello");
        req.setMessage("other");
        assertEquals("hello", FloraContextBuilder.resolveContent(req));
    }

    @Test
    void resolveContent_fallsBackToMessage() {
        ChatbotRequest req = new ChatbotRequest();
        req.setMessage("via message");
        assertEquals("via message", FloraContextBuilder.resolveContent(req));
    }

    @Test
    void resolveContent_legacyEmpty() {
        assertEquals("", FloraContextBuilder.resolveContent(new ChatbotRequest()));
    }
}
