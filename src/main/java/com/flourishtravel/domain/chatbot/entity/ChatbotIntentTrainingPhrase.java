package com.flourishtravel.domain.chatbot.entity;

import com.flourishtravel.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Câu nói mẫu (training phrase) gắn với một intent.
 * User nhắn gần giống phrase → map vào intent đó và dùng response_template + system_action.
 */
@Entity
@Table(name = "chatbot_intent_training_phrase", indexes = {
        @Index(columnList = "intent_id"),
        @Index(columnList = "phrase")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatbotIntentTrainingPhrase extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "intent_id", nullable = false)
    private ChatbotIntent intent;

    @Column(nullable = false, length = 500)
    private String phrase;
}
