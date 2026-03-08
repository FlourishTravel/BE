package com.flourishtravel.domain.chatbot.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flourishtravel.domain.chatbot.dto.ChatbotConfigImportDto;
import com.flourishtravel.domain.chatbot.dto.ChatbotConfigResponseDto;
import com.flourishtravel.domain.chatbot.entity.ChatbotGlobalConfig;
import com.flourishtravel.domain.chatbot.entity.ChatbotIntent;
import com.flourishtravel.domain.chatbot.entity.ChatbotIntentTrainingPhrase;
import com.flourishtravel.domain.chatbot.repository.ChatbotGlobalConfigRepository;
import com.flourishtravel.domain.chatbot.repository.ChatbotIntentRepository;
import com.flourishtravel.domain.chatbot.repository.ChatbotIntentTrainingPhraseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service import cấu hình chatbot và cung cấp data cho AI (intents, training phrases).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotConfigService {

    private static final String GLOBAL_CONFIG_KEY = "default";

    private final ChatbotGlobalConfigRepository globalConfigRepository;
    private final ChatbotIntentRepository intentRepository;
    private final ChatbotIntentTrainingPhraseRepository intentPhraseRepository;
    private final ObjectMapper objectMapper;

    /**
     * Import toàn bộ cấu hình: global + intents + training phrases.
     * Intent đã tồn tại (theo intent_name) sẽ được cập nhật; intent mới sẽ thêm.
     */
    @Transactional
    public ChatbotConfigResponseDto importConfig(ChatbotConfigImportDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Config payload is required");
        }

        // 1. Global config
        ChatbotGlobalConfig global = globalConfigRepository.findByConfigKey(GLOBAL_CONFIG_KEY)
                .orElse(ChatbotGlobalConfig.builder().configKey(GLOBAL_CONFIG_KEY).build());
        global.setChatbotName(dto.getChatbot_name());
        global.setLanguage(dto.getLanguage() != null ? dto.getLanguage() : "vi");
        global.setGlobalInstructions(dto.getGlobal_instructions());
        globalConfigRepository.save(global);

        if (dto.getIntents() == null || dto.getIntents().isEmpty()) {
            return getFullConfig();
        }

        // 2. Intents + training phrases
        int order = 0;
        for (ChatbotConfigImportDto.IntentImportDto idto : dto.getIntents()) {
            if (idto.getIntent_name() == null || idto.getIntent_name().isBlank()) continue;

            ChatbotIntent intent = intentRepository.findByIntentName(idto.getIntent_name())
                    .orElse(ChatbotIntent.builder().intentName(idto.getIntent_name()).build());

            intent.setCategory(idto.getCategory());
            intent.setEntitiesToExtract(toJson(idto.getEntities_to_extract()));
            intent.setSystemAction(toJson(idto.getSystem_action()));
            intent.setResponseTemplate(idto.getResponse_template());
            intent.setContextOutput(idto.getContext_output());
            intent.setSentimentAnalysis(idto.getSentiment_analysis());
            intent.setSentimentThreshold(idto.getSentiment_threshold());
            intent.setContextRequirement(idto.getContext_requirement());
            intent.setSortOrder(order++);
            intentRepository.save(intent);

            // Replace training phrases
            intentPhraseRepository.deleteByIntent_Id(intent.getId());
            if (idto.getTraining_phrases() != null) {
                for (String phrase : idto.getTraining_phrases()) {
                    if (phrase == null || phrase.isBlank()) continue;
                    intentPhraseRepository.save(ChatbotIntentTrainingPhrase.builder()
                            .intent(intent)
                            .phrase(phrase.trim())
                            .build());
                }
            }
        }

        log.info("Chatbot config imported: {} intents", dto.getIntents().size());
        return getFullConfig();
    }

    /**
     * Trả về toàn bộ cấu hình (global + intents + training phrases) cho AI/frontend.
     */
    public ChatbotConfigResponseDto getFullConfig() {
        ChatbotConfigResponseDto.ChatbotConfigResponseDtoBuilder builder = ChatbotConfigResponseDto.builder();

        globalConfigRepository.findByConfigKey(GLOBAL_CONFIG_KEY).ifPresent(g -> {
            builder.chatbotName(g.getChatbotName())
                    .language(g.getLanguage())
                    .globalInstructions(g.getGlobalInstructions());
        });

        List<ChatbotIntent> intents = intentRepository.findAllByOrderBySortOrderAsc();
        List<ChatbotConfigResponseDto.IntentResponseDto> intentDtos = intents.stream()
                .map(this::toIntentResponseDto)
                .collect(Collectors.toList());
        builder.intents(intentDtos);

        return builder.build();
    }

    /**
     * Lấy danh sách intent kèm training phrases (để ChatbotService match user message).
     */
    public List<ChatbotIntentWithPhrases> getIntentsWithPhrases() {
        List<ChatbotIntent> intents = intentRepository.findAllByOrderBySortOrderAsc();
        List<ChatbotIntentWithPhrases> result = new ArrayList<>();
        for (ChatbotIntent intent : intents) {
            List<String> phrases = intentPhraseRepository.findByIntent_IdOrderByPhraseAsc(intent.getId())
                    .stream()
                    .map(ChatbotIntentTrainingPhrase::getPhrase)
                    .collect(Collectors.toList());
            result.add(new ChatbotIntentWithPhrases(intent, phrases));
        }
        return result;
    }

    private ChatbotConfigResponseDto.IntentResponseDto toIntentResponseDto(ChatbotIntent intent) {
        List<String> phrases = intentPhraseRepository.findByIntent_IdOrderByPhraseAsc(intent.getId())
                .stream()
                .map(ChatbotIntentTrainingPhrase::getPhrase)
                .collect(Collectors.toList());

        return ChatbotConfigResponseDto.IntentResponseDto.builder()
                .id(intent.getId().toString())
                .intentName(intent.getIntentName())
                .category(intent.getCategory())
                .entitiesToExtract(fromJsonList(intent.getEntitiesToExtract()))
                .systemAction(fromJsonMap(intent.getSystemAction()))
                .responseTemplate(intent.getResponseTemplate())
                .contextOutput(intent.getContextOutput())
                .sentimentAnalysis(intent.getSentimentAnalysis())
                .sentimentThreshold(intent.getSentimentThreshold())
                .contextRequirement(intent.getContextRequirement())
                .sortOrder(intent.getSortOrder())
                .trainingPhrases(phrases)
                .build();
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("Serialize to JSON failed: {}", e.getMessage());
            return null;
        }
    }

    private List<String> fromJsonList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fromJsonMap(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }

    /**
     * DTO nội bộ: intent + danh sách phrase để match.
     */
    public record ChatbotIntentWithPhrases(ChatbotIntent intent, List<String> phrases) {}
}
