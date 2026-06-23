package com.flourishtravel.domain.chatbot.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.domain.chatbot.dto.ChatbotRequest;
import com.flourishtravel.domain.chatbot.dto.ChatbotResponse;
import com.flourishtravel.domain.chatbot.security.ChatbotSecurityAuditLogger;
import com.flourishtravel.domain.chatbot.security.ChatbotSecurityService;
import com.flourishtravel.domain.flora.recommendation.FloraNearbyChatbotHelper;
import com.flourishtravel.domain.flora.service.FloraContextBuilder;
import com.flourishtravel.domain.chatbot.service.ChatbotService;
import com.flourishtravel.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

/**
 * Chatbot controller — xử lý qua {@link ChatbotService}.
 * Nếu client gửi {@code Authorization: Bearer ...}, BE dùng lịch sử booking/favorite để cá nhân hóa.
 */
@RestController
@RequestMapping("/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;
    private final FloraContextBuilder floraContextBuilder;
    private final ChatbotSecurityService chatbotSecurityService;
    private final ChatbotSecurityAuditLogger auditLogger;
    private final FloraNearbyChatbotHelper nearbyChatbotHelper;

    @PostMapping("/message")
    public ResponseEntity<ApiResponse<ChatbotResponse>> message(
            @RequestBody ChatbotRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID userId = principal != null ? principal.getId() : null;
        auditLogger.chatbotRequestType("message", userId != null);

        Optional<ChatbotResponse> bookingGate = chatbotSecurityService.bookingAccessResponse(request, userId);
        if (bookingGate.isPresent()) {
            return ResponseEntity.ok(ApiResponse.ok(bookingGate.get()));
        }

        ChatbotRequest safeRequest = chatbotSecurityService.prepareRequest(request, userId);
        ChatbotResponse response = chatbotService.processMessage(safeRequest, userId);
        response = floraContextBuilder.enrich(response, safeRequest, userId);
        response = nearbyChatbotHelper.maybeEnhance(safeRequest, response, userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
