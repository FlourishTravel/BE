package com.flourishtravel.domain.chatbot.repository;

import com.flourishtravel.domain.chatbot.entity.ChatbotTrainingPhrase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatbotTrainingPhraseRepository extends JpaRepository<ChatbotTrainingPhrase, UUID> {

    List<ChatbotTrainingPhrase> findAllByOrderByPhraseAsc();

    List<ChatbotTrainingPhrase> findByTopicKey(String topicKey);

    boolean existsByPhraseAndTopicKey(String phrase, String topicKey);
}
