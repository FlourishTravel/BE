package com.flourishtravel.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageViewDto {

    private UUID id;
    private String content;
    private String messageType;
    private Instant createdAt;
    private UUID senderId;
    private String senderName;
    /** Ảnh đại diện người gửi (có thể là path /uploads/...). */
    private String senderAvatarUrl;
    /** ADMIN | TOUR_GUIDE | TRAVELER | FLORA | ... */
    private String senderRole;
    private Boolean isPinned;
    /** Tin nhắn đang được trả lời (nếu có). */
    private ChatReplyPreviewDto replyTo;
    @Builder.Default
    private List<ChatReactionSummaryDto> reactions = new ArrayList<>();
}
