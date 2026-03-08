package com.flourishtravel.domain.chatbot.dto;

import lombok.Data;

@Data
public class ChatbotRequest {

    private String content;
    /** Session identifier (demo gửi "demo-xxx", client thật có thể gửi UUID string). */
    private String sessionId;
    private String userId;
}
