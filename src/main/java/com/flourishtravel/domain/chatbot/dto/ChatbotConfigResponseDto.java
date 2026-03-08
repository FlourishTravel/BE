package com.flourishtravel.domain.chatbot.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO trả về cấu hình chatbot cho API (AI/frontend lấy data training).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatbotConfigResponseDto {

    private String chatbotName;
    private String language;
    private String globalInstructions;
    private List<IntentResponseDto> intents;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class IntentResponseDto {
        private String id;
        private String intentName;
        private String category;
        private List<String> entitiesToExtract;
        private Map<String, Object> systemAction;
        private String responseTemplate;
        private String contextOutput;
        private String sentimentAnalysis;
        private String sentimentThreshold;
        private String contextRequirement;
        private Integer sortOrder;
        private List<String> trainingPhrases;
    }
}
