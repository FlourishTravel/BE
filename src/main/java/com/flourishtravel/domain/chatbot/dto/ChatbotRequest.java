package com.flourishtravel.domain.chatbot.dto;

import lombok.Data;

import java.util.Map;

@Data
public class ChatbotRequest {

    private String content;
    /** Session identifier (demo gửi "demo-xxx", client thật có thể gửi UUID string). */
    private String sessionId;
    private String userId;
    /**
     * State từ response trước (FE bắt buộc gửi lại ở mỗi tin nhắn tiếp theo).
     * Backend merge state.slots vào lượt mới → chatbot trả lời dựa trên câu trước (destination, ngày, intent…).
     */
    private Map<String, Object> state;
}
