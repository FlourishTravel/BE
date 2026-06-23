package com.flourishtravel.domain.chatbot.dto;

import com.flourishtravel.domain.flora.dto.FloraNextMeetingDto;
import com.flourishtravel.domain.flora.dto.FloraSuggestedActionDto;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
public class ChatbotRequest {

    private String content;
    /** Alias for content (Flora clients). */
    private String message;
    private String sessionId;
    private String userId;
    private Map<String, Object> state;

    /** Active booking for in-trip context (optional). */
    private UUID bookingId;
    private Double latitude;
    private Double longitude;
    private String locale;
    private String source;
}
