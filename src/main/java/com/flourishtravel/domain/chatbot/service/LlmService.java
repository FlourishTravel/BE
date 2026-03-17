package com.flourishtravel.domain.chatbot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.InferenceConfiguration;
import software.amazon.awssdk.services.bedrockruntime.model.Message;

import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Gọi LLM để tạo nội dung. Ưu tiên Coze → Bedrock → Gemini → OpenAI.
 */
@Service
@Slf4j
public class LlmService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${app.gemini.api-key:}")
    private String geminiApiKey;

    @Value("${app.gemini.model:gemini-2.0-flash}")
    private String geminiModel;

    @Value("${app.coze.api-key:}")
    private String cozeApiKey;

    @Value("${app.coze.base-url:https://api.coze.com}")
    private String cozeBaseUrl;

    @Value("${app.coze.bot-id:}")
    private String cozeBotId;

    @Value("${app.openai.api-key:}")
    private String openaiApiKey;

    @Value("${app.openai.model:gpt-4o-mini}")
    private String openaiModel;

    @Value("${app.bedrock.region:us-east-1}")
    private String bedrockRegion;

    @Value("${app.bedrock.access-key-id:}")
    private String bedrockAccessKeyId;

    @Value("${app.bedrock.secret-access-key:}")
    private String bedrockSecretAccessKey;

    @Value("${app.bedrock.model-id:anthropic.claude-3-haiku-20240307-v1:0}")
    private String bedrockModelId;

    private volatile BedrockRuntimeClient bedrockClient;

    public LlmService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    /**
     * Gửi prompt, nhận text trả về. Trả null nếu không cấu hình key hoặc lỗi.
     * Thứ tự: Coze → AWS Bedrock → Gemini → OpenAI.
     */
    public String generate(String prompt) {
        if (prompt == null || prompt.isBlank()) return null;
        if (isCozeConfigured()) {
            String out = callCoze(prompt);
            if (out != null) return out;
        }
        if (isBedrockConfigured()) {
            String out = callBedrock(prompt);
            if (out != null) return out;
        }
        if (geminiApiKey != null && !geminiApiKey.isBlank()) {
            String out = callGemini(prompt);
            if (out != null) return out;
        }
        if (openaiApiKey != null && !openaiApiKey.isBlank()) {
            String out = callOpenAI(prompt);
            if (out != null) return out;
        }
        return null;
    }

    private boolean isCozeConfigured() {
        return cozeApiKey != null && !cozeApiKey.isBlank()
                && cozeBotId != null && !cozeBotId.isBlank()
                && cozeBaseUrl != null && !cozeBaseUrl.isBlank();
    }

    private boolean isBedrockConfigured() {
        return bedrockRegion != null && !bedrockRegion.isBlank()
                && bedrockAccessKeyId != null && !bedrockAccessKeyId.isBlank()
                && bedrockSecretAccessKey != null && !bedrockSecretAccessKey.isBlank();
    }

    private BedrockRuntimeClient getBedrockClient() {
        if (bedrockClient == null) {
            synchronized (this) {
                if (bedrockClient == null && isBedrockConfigured()) {
                    bedrockClient = BedrockRuntimeClient.builder()
                            .region(Region.of(bedrockRegion))
                            .credentialsProvider(StaticCredentialsProvider.create(
                                    AwsBasicCredentials.create(bedrockAccessKeyId.trim(), bedrockSecretAccessKey.trim())))
                            .build();
                }
            }
        }
        return bedrockClient;
    }

    @PreDestroy
    public void closeBedrock() {
        if (bedrockClient != null) {
            bedrockClient.close();
            bedrockClient = null;
        }
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

    private static final int GEMINI_RETRY_MAX = 2;
    private static final long GEMINI_RETRY_DELAY_MS = 2000L;

    private String callGemini(String prompt) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + geminiModel + ":generateContent?key=" + geminiApiKey.trim();
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("temperature", 0.3, "maxOutputTokens", 1024)
        );
        for (int attempt = 1; attempt <= GEMINI_RETRY_MAX; attempt++) {
            try {
                @SuppressWarnings("rawtypes")
                Map response = webClient.post()
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
                boolean retryable = e.getStatusCode() != null && (e.getStatusCode().value() == 503 || e.getStatusCode().value() == 429);
                log.warn("Gemini API error: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
                if (retryable && attempt < GEMINI_RETRY_MAX) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(GEMINI_RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                } else {
                    return null;
                }
            } catch (Exception e) {
                log.warn("Gemini call failed", e);
                return null;
            }
        }
        return null;
    }

    private String callOpenAI(String prompt) {
        try {
            Map<String, Object> body = Map.of(
                    "model", openaiModel,
                    "messages", List.of(Map.of("role", "user", "content", prompt)),
                    "temperature", 0.3,
                    "max_tokens", 1024
            );
            @SuppressWarnings("rawtypes")
            Map response = webClient.post()
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

    private String callBedrock(String prompt) {
        BedrockRuntimeClient client = getBedrockClient();
        if (client == null) return null;
        try {
            Message userMessage = Message.builder()
                    .role(ConversationRole.USER)
                    .content(ContentBlock.fromText(prompt))
                    .build();
            ConverseRequest request = ConverseRequest.builder()
                    .modelId(bedrockModelId)
                    .messages(userMessage)
                    .inferenceConfig(InferenceConfiguration.builder()
                            .maxTokens(1024)
                            .temperature(0.3f)
                            .topP(0.9f)
                            .build())
                    .build();
            ConverseResponse response = client.converse(request);
            if (response == null || response.output() == null || response.output().message() == null
                    || response.output().message().content() == null || response.output().message().content().isEmpty()) {
                return null;
            }
            return response.output().message().content().get(0).text();
        } catch (Exception e) {
            log.warn("Bedrock call failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Coze Open API v2 chat. Trả về text, hoặc null nếu lỗi.
     * Docs tham khảo: coze-dev/coze-studio Wiki "API Reference" (Open API v2).
     */
    private String callCoze(String prompt) {
        try {
            String base = normalizeCozeBaseUrl(cozeBaseUrl);
            String url = base + "/open_api/v2/chat";

            Map<String, Object> body = Map.of(
                    "bot_id", cozeBotId.trim(),
                    "user_id", "be-user",
                    "stream", false,
                    "additional_messages", List.of(Map.of(
                            "role", "user",
                            "type", "question",
                            "content_type", "text",
                            "content", prompt
                    ))
            );

            @SuppressWarnings("rawtypes")
            Map response = webClient.post()
                    .uri(url)
                    .header("Authorization", "Bearer " + cozeApiKey.trim())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) return null;

            // Try multiple shapes to be resilient across versions
            Object dataObj = response.get("data");
            if (dataObj instanceof Map<?, ?> data) {
                Object messagesObj = data.get("messages");
                String text = extractCozeTextFromMessages(messagesObj);
                if (text != null) return text;
                Object answer = data.get("answer");
                if (answer != null) return String.valueOf(answer);
            }

            Object messagesObj = response.get("messages");
            String text = extractCozeTextFromMessages(messagesObj);
            if (text != null) return text;

            Object msgObj = response.get("msg");
            if (msgObj != null) return String.valueOf(msgObj);

            return null;
        } catch (WebClientResponseException e) {
            log.warn("Coze API error: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.warn("Coze call failed", e);
            return null;
        }
    }

    /**
     * Coze Studio dùng domain https://www.coze.com, nhưng Open API thường nằm ở https://api.coze.com.
     * Cho phép set COZE_BASE_URL = https://www.coze.com (hoặc https://coze.com) trong .env mà vẫn gọi đúng endpoint API.
     */
    private static String normalizeCozeBaseUrl(String raw) {
        String url = (raw == null ? "" : raw.trim());
        if (url.isEmpty()) return "https://api.coze.com";
        while (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        String lower = url.toLowerCase();
        if ("https://www.coze.com".equals(lower) || "http://www.coze.com".equals(lower)
                || "https://coze.com".equals(lower) || "http://coze.com".equals(lower)) {
            return "https://api.coze.com";
        }
        return url;
    }

    private String extractCozeTextFromMessages(Object messagesObj) {
        if (!(messagesObj instanceof List<?> list) || list.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> m)) continue;
            Object content = m.get("content");
            if (content != null) sb.append(String.valueOf(content));
        }
        String out = sb.toString().trim();
        return out.isEmpty() ? null : out;
    }
}
