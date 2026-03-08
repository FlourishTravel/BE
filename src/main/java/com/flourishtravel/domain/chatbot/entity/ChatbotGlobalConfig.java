package com.flourishtravel.domain.chatbot.entity;

import com.flourishtravel.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Cấu hình toàn cục của chatbot (1 bản ghi: tên, ngôn ngữ, hướng dẫn).
 */
@Entity
@Table(name = "chatbot_global_config", indexes = @Index(columnList = "config_key", unique = true))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatbotGlobalConfig extends BaseEntity {

    @Column(name = "config_key", nullable = false, length = 50)
    private String configKey;

    @Column(name = "chatbot_name", length = 200)
    private String chatbotName;

    @Column(length = 10)
    private String language;

    @Column(name = "global_instructions", columnDefinition = "TEXT")
    private String globalInstructions;
}
