package com.flourishtravel.domain.chatbot.entity;

import com.flourishtravel.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

/**
 * Intent của chatbot (personalized_recommendation, policy_cancellation, in_tour_crisis_handling...).
 * Chứa cấu hình system_action, response_template, entities để trả lời đúng tình huống.
 */
@Entity
@Table(name = "chatbot_intent", indexes = {
        @Index(columnList = "intent_name", unique = true),
        @Index(columnList = "category")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatbotIntent extends BaseEntity {

    @Column(name = "intent_name", nullable = false, length = 100)
    private String intentName;

    @Column(length = 80)
    private String category;

    /** JSON array: ["budget", "duration", "region", "vibe"] */
    @Column(name = "entities_to_extract", columnDefinition = "TEXT")
    private String entitiesToExtract;

    /** JSON: {"type":"database_query","api_endpoint":"/api/tours/search"} */
    @Column(name = "system_action", columnDefinition = "TEXT")
    private String systemAction;

    @Column(name = "response_template", columnDefinition = "TEXT")
    private String responseTemplate;

    @Column(name = "context_output", length = 80)
    private String contextOutput;

    @Column(name = "sentiment_analysis", length = 50)
    private String sentimentAnalysis;

    @Column(name = "sentiment_threshold", length = 30)
    private String sentimentThreshold;

    @Column(name = "context_requirement", length = 80)
    private String contextRequirement;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @OneToMany(mappedBy = "intent", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("phrase ASC")
    private List<ChatbotIntentTrainingPhrase> trainingPhrases;
}
