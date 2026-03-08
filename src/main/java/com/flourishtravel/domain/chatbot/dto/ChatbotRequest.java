package com.flourishtravel.domain.chatbot.dto;

import lombok.Data;

import java.util.Map;

@Data
public class ChatbotRequest {

    private String content;
    /** Session identifier (demo gửi "demo-xxx", client thật có thể gửi UUID string). */
    private String sessionId;
    private String userId;
    /** State từ response trước (FE gửi lại để merge slots đa vòng). */
    private Map<String, Object> state;
}
