package com.flourishtravel.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flourishtravel.domain.chatbot.dto.ChatbotConfigImportDto;
import com.flourishtravel.domain.chatbot.service.ChatbotConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;

/**
 * Tự động import cấu hình chatbot từ chatbot-config-sample.json khi ứng dụng khởi động.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatbotConfigSeeder {

    private static final String CONFIG_FILE = "chatbot-config-sample.json";

    private final ChatbotConfigService chatbotConfigService;
    private final ObjectMapper objectMapper;

    @EventListener(ApplicationReadyEvent.class)
    @Order(7)
    @Transactional
    public void seed() {
        try {
            ClassPathResource resource = new ClassPathResource(CONFIG_FILE);
            if (!resource.exists()) {
                log.debug("Chatbot config file not found: {}", CONFIG_FILE);
                return;
            }
            try (InputStream is = resource.getInputStream()) {
                ChatbotConfigImportDto dto = objectMapper.readValue(is, ChatbotConfigImportDto.class);
                if (dto.getIntents() != null && !dto.getIntents().isEmpty()) {
                    chatbotConfigService.importConfig(dto);
                    log.info("Chatbot config imported from {} ({} intents)", CONFIG_FILE, dto.getIntents().size());
                }
            }
        } catch (Exception e) {
            log.warn("Could not import chatbot config from {}: {}", CONFIG_FILE, e.getMessage());
        }
    }
}
