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
import java.util.concurrent.TimeUnit;

/**
 * Gọi LLM qua OpenRouter (OpenAI-compatible API). Model mặc định: Google Gemini 3 Flash Preview.
 */
@Service
@Slf4j
public class LlmService {

    private static final int RETRY_MAX = 2;
    private static final long RETRY_DELAY_MS = 2000L;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${app.openrouter.api-key:}")
    private String openRouterApiKey;

    @Value("${app.openrouter.model:google/gemini-3-flash-preview}")
    private String openRouterModel;

    @Value("${app.openrouter.base-url:https://openrouter.ai/api/v1}")
    private String openRouterBaseUrl;

    @Value("${app.openrouter.http-referer:https://flourishtravel.com}")
    private String httpReferer;

    @Value("${app.openrouter.app-title:FlourishTravel}")
    private String appTitle;

    public LlmService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    /**
     * Gửi prompt, nhận text trả về. Trả null nếu không cấu hình key hoặc lỗi.
     */
    public String generate(String prompt) {
        if (prompt == null || prompt.isBlank()) return null;
        if (openRouterApiKey == null || openRouterApiKey.isBlank()) {
            log.debug("OpenRouter API key not configured");
            return null;
        }
        return callOpenRouter(prompt);
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

    private String callOpenRouter(String prompt) {
        String url = normalizeBaseUrl(openRouterBaseUrl) + "/chat/completions";
        Map<String, Object> body = Map.of(
                "model", openRouterModel,
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "temperature", 0.3,
                "max_tokens", 1024
        );

        for (int attempt = 1; attempt <= RETRY_MAX; attempt++) {
            try {
                @SuppressWarnings("rawtypes")
                Map response = webClient.post()
                        .uri(url)
                        .header("Authorization", "Bearer " + openRouterApiKey.trim())
                        .header("HTTP-Referer", httpReferer)
                        .header("X-Title", appTitle)
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
                boolean retryable = e.getStatusCode() != null
                        && (e.getStatusCode().value() == 503 || e.getStatusCode().value() == 429);
                log.warn("OpenRouter API error: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
                if (retryable && attempt < RETRY_MAX) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                } else {
                    return null;
                }
            } catch (Exception e) {
                log.warn("OpenRouter call failed", e);
                return null;
            }
        }
        return null;
    }

    private static String normalizeBaseUrl(String raw) {
        String url = (raw == null ? "" : raw.trim());
        if (url.isEmpty()) return "https://openrouter.ai/api/v1";
        while (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        return url;
    }
}
