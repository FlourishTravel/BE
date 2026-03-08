package com.flourishtravel.domain.chatbot.entity;

import com.flourishtravel.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Nội dung chính sách / FAQ để chatbot trả lời (hủy tour, đổi ngày, hoàn tiền...).
 */
@Entity
@Table(name = "policy_faq", indexes = @Index(columnList = "topic_key", unique = true))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyFaq extends BaseEntity {

    @Column(name = "topic_key", nullable = false, length = 50)
    private String topicKey;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;
}
