package com.flourishtravel.domain.chatbot.repository;

import com.flourishtravel.domain.chatbot.entity.ChatbotGlobalConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatbotGlobalConfigRepository extends JpaRepository<ChatbotGlobalConfig, UUID> {

    Optional<ChatbotGlobalConfig> findByConfigKey(String configKey);
}
