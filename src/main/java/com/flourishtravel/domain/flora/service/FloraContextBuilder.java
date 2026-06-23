package com.flourishtravel.domain.flora.service;

import com.flourishtravel.domain.chatbot.dto.ChatbotRequest;
import com.flourishtravel.domain.chatbot.dto.ChatbotResponse;
import com.flourishtravel.domain.chatbot.service.ChatbotUserContextService;
import com.flourishtravel.domain.flora.dto.FloraJourneyDto;
import com.flourishtravel.domain.flora.dto.FloraSuggestedActionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FloraContextBuilder {

    private final ChatbotUserContextService chatbotUserContextService;
    private final UserTravelPreferenceService preferenceService;
    private final FloraPrivacyService privacyService;
    private final FloraJourneyService journeyService;

    public String buildCombinedHint(ChatbotRequest request, UUID userId, Map<String, Object> previousState) {
        StringBuilder sb = new StringBuilder();
        String sessionHint = buildSessionHint(previousState);
        if (!sessionHint.isBlank()) sb.append(sessionHint);

        if (userId != null && privacyService.hasPersonalizationConsent(userId)) {
            String profile = chatbotUserContextService.buildProfileHint(userId);
            if (!profile.isBlank()) {
                if (!sb.isEmpty()) sb.append("\n");
                sb.append(privacyService.sanitizeForPrompt(profile));
            }
            String prefs = preferenceService.buildPreferenceHint(userId);
            if (!prefs.isBlank()) {
                if (!sb.isEmpty()) sb.append("\n");
                sb.append(privacyService.sanitizeForPrompt(prefs));
            }
        }

        UUID bookingId = request.getBookingId();
        if (bookingId != null && userId != null) {
            appendJourneyContext(sb, bookingId, userId);
        }

        if (request.getLatitude() != null && request.getLongitude() != null) {
            if (!sb.isEmpty()) sb.append("\n");
            sb.append("Vị trí GPS user (đã cấp quyền): lat=").append(request.getLatitude())
                    .append(", lon=").append(request.getLongitude()).append("\n");
        }

        return sb.toString();
    }

    public ChatbotResponse enrich(ChatbotResponse response, ChatbotRequest request, UUID userId) {
        if (response == null) return null;
        ChatbotResponse.ChatbotResponseBuilder builder = response.toBuilder();
        if (response.getAnswer() == null && response.getReply() != null) {
            builder.answer(response.getReply());
        }
        if (response.getQuickReplies() == null || response.getQuickReplies().isEmpty()) {
            builder.quickReplies(com.flourishtravel.domain.flora.FloraQuickActions.defaults());
        }

        UUID bookingId = request.getBookingId();
        if (bookingId != null && userId != null) {
            try {
                FloraJourneyDto journey = journeyService.getJourney(bookingId, userId);
                if (journey.getNextMeeting() != null) {
                    builder.nextMeeting(journey.getNextMeeting());
                }
                List<FloraSuggestedActionDto> actions = new ArrayList<>();
                actions.add(FloraSuggestedActionDto.builder()
                        .type("OPEN_MAP")
                        .label("Chỉ đường về điểm tập trung")
                        .payload(journey.getMeetingPoint())
                        .build());
                builder.suggestedActions(actions);
            } catch (Exception ignored) {
            }
        }
        return builder.build();
    }

    private void appendJourneyContext(StringBuilder sb, UUID bookingId, UUID userId) {
        try {
            FloraJourneyDto journey = journeyService.getJourney(bookingId, userId);
            if (!sb.isEmpty()) sb.append("\n");
            sb.append("Ngữ cảnh chuyến đi hiện tại:\n");
            if (journey.getTourTitle() != null) {
                sb.append("- Tour: ").append(journey.getTourTitle()).append("\n");
            }
            if (journey.getCurrentScheduleItem() != null) {
                sb.append("- Hôm nay: ").append(journey.getCurrentScheduleItem().getTitle()).append("\n");
            }
            if (journey.getNextMeeting() != null && journey.getNextMeeting().getLocation() != null) {
                sb.append("- Điểm tập trung: ").append(journey.getNextMeeting().getLocation());
                if (journey.getMinutesUntilGathering() != null) {
                    sb.append(" (còn ~").append(journey.getMinutesUntilGathering()).append(" phút)");
                }
                sb.append("\n");
            }
            if (journey.getWeatherSummary() != null) {
                sb.append("- Thời tiết: ").append(journey.getWeatherSummary()).append("\n");
            }
        } catch (Exception ignored) {
        }
    }

    private static String buildSessionHint(Map<String, Object> previousState) {
        if (previousState == null) return "";
        Object slotsObj = previousState.get("slots");
        if (!(slotsObj instanceof Map<?, ?> m) || m.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("Context từ lượt trước (đã biết): ");
        m.forEach((k, v) -> {
            if (v != null && !"null".equals(String.valueOf(v))) sb.append(k).append("=").append(v).append("; ");
        });
        return sb.append("\n").toString();
    }

    public static String resolveContent(ChatbotRequest request) {
        if (request.getContent() != null && !request.getContent().isBlank()) {
            return request.getContent().trim();
        }
        if (request.getMessage() != null && !request.getMessage().isBlank()) {
            return request.getMessage().trim();
        }
        return "";
    }
}
