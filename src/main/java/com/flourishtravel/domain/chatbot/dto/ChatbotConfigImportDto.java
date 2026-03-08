package com.flourishtravel.domain.chatbot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * DTO nhận JSON import cấu hình chatbot (tên, ngôn ngữ, hướng dẫn, danh sách intent).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatbotConfigImportDto {

    private String chatbot_name;
    private String language;
    private String global_instructions;
    private List<IntentImportDto> intents;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IntentImportDto {
        private String intent_name;
        private String category;
        private List<String> training_phrases;
        private List<String> entities_to_extract;
        private SystemActionImportDto system_action;
        private String response_template;
        private String context_output;
        private String sentiment_analysis;
        private String sentiment_threshold;
        private String context_requirement;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SystemActionImportDto {
        private String type;
        private String api_endpoint;
        private String trigger;
        private String priority;
    }
}
