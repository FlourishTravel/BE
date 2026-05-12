package com.flourishtravel.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
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
    /** ADMIN | TOUR_GUIDE | TRAVELER | ... */
    private String senderRole;
    private Boolean isPinned;
}
