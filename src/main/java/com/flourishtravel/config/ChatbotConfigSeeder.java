package com.flourishtravel.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flourishtravel.domain.chatbot.dto.ChatbotConfigImportDto;
import com.flourishtravel.domain.chatbot.repository.ChatbotIntentRepository;
import com.flourishtravel.domain.chatbot.service.ChatbotConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * Import cấu hình chatbot từ {@code chatbot-config-sample.json} khi bật {@code app.seed.chatbot-config-on-startup}.
 * Mặc định tắt trên ECS (profile cloud). Idempotent: bỏ qua nếu DB đã có intent.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.seed.chatbot-config-on-startup", havingValue = "true")
public class ChatbotConfigSeeder {

    private static final String CONFIG_FILE = "chatbot-config-sample.json";

    private final ChatbotConfigService chatbotConfigService;
    private final ChatbotIntentRepository chatbotIntentRepository;
    private final ObjectMapper objectMapper;

    @EventListener(ApplicationReadyEvent.class)
    @Order(7)
    public void seed() {
        if (chatbotIntentRepository.count() > 0) {
            log.debug("Chatbot intents already present (count={}), skip config import", chatbotIntentRepository.count());
            return;
        }

        ClassPathResource resource = new ClassPathResource(CONFIG_FILE);
        if (!resource.exists()) {
            log.debug("Chatbot config file not found: {}", CONFIG_FILE);
            return;
        }

        try (InputStream is = resource.getInputStream()) {
            ChatbotConfigImportDto dto = objectMapper.readValue(is, ChatbotConfigImportDto.class);
            if (dto.getIntents() == null || dto.getIntents().isEmpty()) {
                return;
            }
            if (alreadySeeded(dto)) {
                log.info("Chatbot config already seeded, skipping {}", CONFIG_FILE);
                return;
            }
            chatbotConfigService.importConfig(dto);
            log.info("Chatbot config imported from {} ({} intents)", CONFIG_FILE, dto.getIntents().size());
        } catch (DataIntegrityViolationException | PessimisticLockingFailureException e) {
            log.warn("Chatbot config import skipped (concurrent or duplicate): {}", e.getMessage());
        } catch (Exception e) {
            log.warn("Could not import chatbot config from {}: {}", CONFIG_FILE, e.getMessage());
        }
    }

    private boolean alreadySeeded(ChatbotConfigImportDto dto) {
        for (ChatbotConfigImportDto.IntentImportDto intent : dto.getIntents()) {
            if (intent.getIntent_name() == null || intent.getIntent_name().isBlank()) {
                continue;
            }
            if (chatbotIntentRepository.findByIntentName(intent.getIntent_name()).isPresent()) {
                return true;
            }
        }
        return false;
    }
}
