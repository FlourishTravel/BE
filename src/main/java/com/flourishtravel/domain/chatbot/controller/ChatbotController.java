package com.flourishtravel.domain.chatbot.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.domain.chatbot.dto.ChatbotRequest;
import com.flourishtravel.domain.chatbot.dto.ChatbotResponse;
import com.flourishtravel.domain.chatbot.service.ChatbotService;
import com.flourishtravel.domain.chatbot.service.N8nChatbotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Chatbot controller — ưu tiên n8n nếu được bật và có webhook URL,
 * tự động fallback về ChatbotService (Gemini/OpenAI) nếu n8n lỗi hoặc chưa cấu hình.
 */
@RestController
@RequestMapping("/chatbot")
@RequiredArgsConstructor
@Slf4j
public class ChatbotController {

    private final ChatbotService chatbotService;
    private final N8nChatbotService n8nChatbotService;

    @PostMapping("/message")
    public ResponseEntity<ApiResponse<ChatbotResponse>> message(@RequestBody ChatbotRequest request) {

        // ── Ưu tiên n8n nếu đã cấu hình webhook URL ──────────────────────────
        if (n8nChatbotService.isAvailable()) {
            try {
                ChatbotResponse response = n8nChatbotService.call(request);
                log.debug("Chatbot: dùng n8n thành công");
                return ResponseEntity.ok(ApiResponse.ok(response));
            } catch (Exception e) {
                log.warn("N8n webhook thất bại, fallback về ChatbotService cũ. Lỗi: {}", e.getMessage());
            }
        }

        // ── Fallback: ChatbotService (Gemini / OpenAI / rule-based) ──────────
        ChatbotResponse response = chatbotService.processMessage(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
