package com.flourishtravel.domain.chatbot.repository;

import com.flourishtravel.domain.chatbot.entity.ChatbotIntent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatbotIntentRepository extends JpaRepository<ChatbotIntent, UUID> {

    Optional<ChatbotIntent> findByIntentName(String intentName);

    List<ChatbotIntent> findAllByOrderBySortOrderAsc();
}
