package com.flourishtravel.domain.chat.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.domain.chat.entity.Message;
import com.flourishtravel.domain.chat.entity.MessageReaction;
import com.flourishtravel.domain.chat.service.ChatService;
import com.flourishtravel.security.UserPrincipal;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ApiResponse<List<Message>>> getRoomMessages(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID roomId,
            @RequestParam(required = false) Integer limit) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        List<Message> messages = chatService.getRoomMessages(roomId, principal.getId(), limit);
        return ResponseEntity.ok(ApiResponse.ok(messages));
    }

    @PatchMapping("/messages/{id}/pin")
    public ResponseEntity<ApiResponse<Message>> pinMessage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") UUID messageId) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        Message msg = chatService.pinMessage(messageId, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Đã ghim tin nhắn", msg));
    }

    @PatchMapping("/messages/{id}/unpin")
    public ResponseEntity<ApiResponse<Message>> unpinMessage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") UUID messageId) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        Message msg = chatService.unpinMessage(messageId, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Đã bỏ ghim", msg));
    }

    @PostMapping("/messages/{messageId}/reactions")
    public ResponseEntity<ApiResponse<MessageReaction>> addReaction(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID messageId,
            @RequestBody(required = false) ReactionRequest body) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        String type = body != null && body.getReactionType() != null ? body.getReactionType() : "like";
        MessageReaction reaction = chatService.addReaction(messageId, principal.getId(), type);
        return ResponseEntity.ok(ApiResponse.ok(reaction));
    }

    @Data
    public static class ReactionRequest {
        private String reactionType;
    }
}
