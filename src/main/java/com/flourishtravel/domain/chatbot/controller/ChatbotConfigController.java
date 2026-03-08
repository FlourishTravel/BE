package com.flourishtravel.domain.chatbot.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.domain.chatbot.dto.ChatbotConfigImportDto;
import com.flourishtravel.domain.chatbot.dto.ChatbotConfigResponseDto;
import com.flourishtravel.domain.chatbot.service.ChatbotConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * API cấu hình chatbot: import training (intents + phrases) và lấy data cho AI.
 */
@RestController
@RequestMapping("/chatbot/config")
@RequiredArgsConstructor
public class ChatbotConfigController {

    private final ChatbotConfigService chatbotConfigService;

    /**
     * Import cấu hình chatbot (JSON: chatbot_name, language, global_instructions, intents với training_phrases).
     * POST body = file JSON như ví dụ trong yêu cầu.
     */
    @PostMapping("/import")
    public ResponseEntity<ApiResponse<ChatbotConfigResponseDto>> importConfig(@RequestBody ChatbotConfigImportDto body) {
        ChatbotConfigResponseDto result = chatbotConfigService.importConfig(body);
        return ResponseEntity.ok(ApiResponse.ok("Import thành công", result));
    }

    /**
     * Lấy toàn bộ cấu hình (global + intents + training phrases) để AI/frontend dùng.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<ChatbotConfigResponseDto>> getFullConfig() {
        ChatbotConfigResponseDto config = chatbotConfigService.getFullConfig();
        return ResponseEntity.ok(ApiResponse.ok(config));
    }

    /**
     * Lấy danh sách intents (kèm training phrases) — dùng cho training hoặc debug.
     */
    @GetMapping("/intents")
    public ResponseEntity<ApiResponse<ChatbotConfigResponseDto>> getIntents() {
        ChatbotConfigResponseDto config = chatbotConfigService.getFullConfig();
        return ResponseEntity.ok(ApiResponse.ok(config));
    }
}
