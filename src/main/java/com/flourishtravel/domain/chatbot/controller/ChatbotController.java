package com.flourishtravel.domain.chatbot.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.domain.chatbot.dto.ChatbotRequest;
import com.flourishtravel.domain.chatbot.dto.ChatbotResponse;
import com.flourishtravel.domain.chatbot.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/message")
    public ResponseEntity<ApiResponse<ChatbotResponse>> message(@RequestBody ChatbotRequest request) {
        ChatbotResponse response = chatbotService.processMessage(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
