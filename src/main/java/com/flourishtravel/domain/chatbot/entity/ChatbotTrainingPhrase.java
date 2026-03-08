package com.flourishtravel.domain.chatbot.entity;

import com.flourishtravel.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Câu hỏi / câu nói mẫu của khách để map vào FAQ (training data cho chatbot).
 * Khi user nhắn gần giống phrase → trả FAQ theo topicKey.
 */
@Entity
@Table(name = "chatbot_training_phrase", indexes = {
        @Index(columnList = "topic_key"),
        @Index(columnList = "phrase")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatbotTrainingPhrase extends BaseEntity {

    @Column(nullable = false, length = 500)
    private String phrase;

    @Column(name = "topic_key", nullable = false, length = 50)
    private String topicKey;
}
