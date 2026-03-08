package com.flourishtravel.domain.chatbot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * Gọi LLM (Gemini hoặc OpenAI) để tạo nội dung. Ưu tiên Gemini nếu có key.
 */
@Service
@Slf4j
public class LlmService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${app.gemini.api-key:}")
    private String geminiApiKey;

    @Value("${app.gemini.model:gemini-1.5-flash}")
    private String geminiModel;

    @Value("${app.openai.api-key:}")
    private String openaiApiKey;

    @Value("${app.openai.model:gpt-4o-mini}")
    private String openaiModel;

    public LlmService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    /**
     * Gửi prompt, nhận text trả về. Trả null nếu không cấu hình key hoặc lỗi.
     */
    public String generate(String prompt) {
        if (prompt == null || prompt.isBlank()) return null;
        if (geminiApiKey != null && !geminiApiKey.isBlank()) {
            return callGemini(prompt);
        }
        if (openaiApiKey != null && !openaiApiKey.isBlank()) {
            return callOpenAI(prompt);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> generateJson(String prompt) {
        String raw = generate(prompt);
        if (raw == null || raw.isBlank()) return null;
        String cleaned = raw.trim();
        if (cleaned.startsWith("```json")) cleaned = cleaned.substring(7);
        if (cleaned.startsWith("```")) cleaned = cleaned.substring(3);
        if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.length() - 3);
        cleaned = cleaned.trim();
        try {
            return objectMapper.readValue(cleaned, Map.class);
        } catch (Exception e) {
            log.warn("LLM response is not valid JSON: {}", cleaned.substring(0, Math.min(200, cleaned.length())));
            return null;
        }
    }

    private String callGemini(String prompt) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + geminiModel + ":generateContent?key=" + geminiApiKey.trim();
        try {
            Map<String, Object> body = Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                    "generationConfig", Map.of("temperature", 0.3, "maxOutputTokens", 1024)
            );
            Map<String, Object> response = webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            if (response == null) return null;
            var candidates = (List<?>) response.get("candidates");
            if (candidates == null || candidates.isEmpty()) return null;
            var content = ((Map<?, ?>) candidates.get(0)).get("content");
            if (content == null) return null;
            var parts = (List<?>) ((Map<?, ?>) content).get("parts");
            if (parts == null || parts.isEmpty()) return null;
            var text = ((Map<?, ?>) parts.get(0)).get("text");
            return text != null ? text.toString() : null;
        } catch (WebClientResponseException e) {
            log.warn("Gemini API error: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.warn("Gemini call failed", e);
            return null;
        }
    }

    private String callOpenAI(String prompt) {
        try {
            Map<String, Object> body = Map.of(
                    "model", openaiModel,
                    "messages", List.of(Map.of("role", "user", "content", prompt)),
                    "temperature", 0.3,
                    "max_tokens", 1024
            );
            Map<String, Object> response = webClient.post()
                    .uri("https://api.openai.com/v1/chat/completions")
                    .header("Authorization", "Bearer " + openaiApiKey.trim())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            if (response == null) return null;
            var choices = (List<?>) response.get("choices");
            if (choices == null || choices.isEmpty()) return null;
            var message = ((Map<?, ?>) choices.get(0)).get("message");
            if (message == null) return null;
            var content = ((Map<?, ?>) message).get("content");
            return content != null ? content.toString() : null;
        } catch (WebClientResponseException e) {
            log.warn("OpenAI API error: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.warn("OpenAI call failed", e);
            return null;
        }
    }
}
