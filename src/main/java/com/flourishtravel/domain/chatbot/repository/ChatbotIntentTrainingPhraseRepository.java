package com.flourishtravel.domain.chatbot.repository;

import com.flourishtravel.domain.chatbot.entity.ChatbotIntentTrainingPhrase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatbotIntentTrainingPhraseRepository extends JpaRepository<ChatbotIntentTrainingPhrase, UUID> {

    List<ChatbotIntentTrainingPhrase> findByIntent_IdOrderByPhraseAsc(UUID intentId);

    void deleteByIntent_Id(UUID intentId);
}
