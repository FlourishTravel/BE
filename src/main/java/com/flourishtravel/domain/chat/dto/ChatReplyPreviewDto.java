package com.flourishtravel.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatReplyPreviewDto {

    private UUID id;
    private UUID senderId;
    private String senderName;
    private String content;
}
