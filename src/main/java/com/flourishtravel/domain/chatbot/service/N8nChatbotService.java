package com.flourishtravel.domain.chatbot.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flourishtravel.config.N8nProperties;
import com.flourishtravel.domain.chatbot.dto.ChatbotRequest;
import com.flourishtravel.domain.chatbot.dto.ChatbotResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Service gọi n8n workflow qua webhook để xử lý chatbot.
 * <p>
 * Request gửi lên n8n (JSON body):
 * <pre>
 * {
 *   "content":   "Câu hỏi của user",
 *   "sessionId": "session-xxx",
 *   "userId":    "user-uuid hoặc null",
 *   "state":     { ... context từ lượt trước ... }
 * }
 * </pre>
 * <p>
 * Response n8n trả về (Spring Boot expect):
 * <pre>
 * {
 *   "reply":        "Câu trả lời",
 *   "tours":        [ { "id","title","slug","price","durationDays","imageUrl" } ],
 *   "quickReplies": [ { "label","payload" } ],
 *   "state":        { ... state để FE gửi lại lượt sau ... }
 * }
 * </pre>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class N8nChatbotService {

    private final N8nProperties n8nProperties;
    private final ObjectMapper objectMapper;

    /**
     * Trả về true nếu n8n được bật VÀ webhookUrl đã được cấu hình.
     */
    public boolean isAvailable() {
        return n8nProperties.isEnabled()
                && n8nProperties.getWebhookUrl() != null
                && !n8nProperties.getWebhookUrl().isBlank();
    }

    /**
     * Gọi n8n webhook, trả về {@link ChatbotResponse}.
     * Ném RuntimeException nếu n8n lỗi hoặc trả về format không hợp lệ.
     */
    public ChatbotResponse call(ChatbotRequest request) {
        String webhookUrl = n8nProperties.getWebhookUrl();
        log.debug("N8n call → {} | content={}", webhookUrl, request.getContent());

        try {
            // Serialize request → JSON
            String requestBody = objectMapper.writeValueAsString(request);

            // Build HTTP request
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .timeout(Duration.ofSeconds(n8nProperties.getTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            int statusCode = httpResponse.statusCode();
            String responseBody = httpResponse.body();
            log.debug("N8n response status={} body={}", statusCode, responseBody);

            if (statusCode < 200 || statusCode >= 300) {
                log.warn("N8n webhook trả về HTTP {}: {}", statusCode, responseBody);
                throw new RuntimeException("N8n webhook HTTP error: " + statusCode);
            }

            if (responseBody == null || responseBody.isBlank()) {
                throw new RuntimeException("N8n webhook trả về body rỗng");
            }

            return parseN8nResponse(responseBody);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("N8n webhook call thất bại: {}", e.getMessage(), e);
            throw new RuntimeException("N8n webhook call thất bại: " + e.getMessage(), e);
        }
    }

    /**
     * Parse JSON response từ n8n thành {@link ChatbotResponse}.
     * Hỗ trợ:
     * <ul>
     *   <li>n8n trả về object đơn: {@code { "reply": "...", ... }}</li>
     *   <li>n8n trả về array (Respond to Webhook thường wrap trong array): {@code [{ "reply": "..." }]}</li>
     * </ul>
     */
    @SuppressWarnings("unchecked")
    private ChatbotResponse parseN8nResponse(String body) {
        try {
            // n8n Respond to Webhook đôi khi trả array [{ ... }]
            if (body.trim().startsWith("[")) {
                List<Map<String, Object>> list = objectMapper.readValue(body, new TypeReference<>() {});
                if (list == null || list.isEmpty()) {
                    throw new RuntimeException("N8n trả array rỗng");
                }
                return mapToResponse(list.get(0));
            }

            Map<String, Object> map = objectMapper.readValue(body, new TypeReference<>() {});
            return mapToResponse(map);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Parse n8n response thất bại. Body: {}", body, e);
            throw new RuntimeException("Parse n8n response thất bại: " + e.getMessage(), e);
        }
    }

    /**
     * Map raw JSON map → {@link ChatbotResponse}.
     * Các field mà n8n không trả sẽ là null/rỗng.
     */
    @SuppressWarnings("unchecked")
    private ChatbotResponse mapToResponse(Map<String, Object> map) {
        // reply
        String reply = map.get("reply") instanceof String s ? s : null;
        if (reply == null || reply.isBlank()) {
            reply = "Mình đã ghi nhận, bạn cần thêm thông tin gì không?";
        }

        // tours
        List<ChatbotResponse.TourCard> tours = List.of();
        Object toursObj = map.get("tours");
        if (toursObj instanceof List<?> rawList) {
            tours = rawList.stream()
                    .filter(item -> item instanceof Map)
                    .map(item -> {
                        Map<String, Object> t = (Map<String, Object>) item;
                        return ChatbotResponse.TourCard.builder()
                                .id(getString(t, "id"))
                                .title(getString(t, "title"))
                                .slug(getString(t, "slug"))
                                .price(getLong(t, "price"))
                                .durationDays(getInt(t, "durationDays"))
                                .imageUrl(getString(t, "imageUrl"))
                                .build();
                    })
                    .toList();
        }

        // quickReplies
        List<ChatbotResponse.QuickReply> quickReplies = List.of();
        Object qrObj = map.get("quickReplies");
        if (qrObj instanceof List<?> rawList) {
            quickReplies = rawList.stream()
                    .filter(item -> item instanceof Map)
                    .map(item -> {
                        Map<String, Object> q = (Map<String, Object>) item;
                        String label = getString(q, "label");
                        String payload = getString(q, "payload");
                        if (label == null) return null;
                        return ChatbotResponse.QuickReply.builder()
                                .label(label)
                                .payload(payload != null ? payload : label)
                                .build();
                    })
                    .filter(q -> q != null)
                    .toList();
        }

        // state (FE gửi lại lượt sau)
        Map<String, Object> state = null;
        Object stateObj = map.get("state");
        if (stateObj instanceof Map<?, ?> rawState) {
            state = (Map<String, Object>) rawState;
        }

        return ChatbotResponse.builder()
                .reply(reply)
                .tours(tours)
                .quickReplies(quickReplies.isEmpty() ? defaultQuickReplies() : quickReplies)
                .state(state)
                .build();
    }

    private List<ChatbotResponse.QuickReply> defaultQuickReplies() {
        return List.of(
                ChatbotResponse.QuickReply.builder().label("Tour biển 3 ngày").payload("Tour biển 3 ngày").build(),
                ChatbotResponse.QuickReply.builder().label("Chính sách hủy tour").payload("Chính sách hủy tour").build()
        );
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static String getString(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v != null ? v.toString() : null;
    }

    private static Long getLong(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v instanceof Number n) return n.longValue();
        if (v instanceof String s) {
            try { return Long.parseLong(s); } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private static Integer getInt(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v instanceof Number n) return n.intValue();
        return null;
    }
}
