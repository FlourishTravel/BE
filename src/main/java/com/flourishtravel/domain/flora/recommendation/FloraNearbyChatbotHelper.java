package com.flourishtravel.domain.flora.recommendation;

import com.flourishtravel.domain.chatbot.dto.ChatbotRequest;
import com.flourishtravel.domain.chatbot.dto.ChatbotResponse;
import com.flourishtravel.domain.flora.FloraRecommendationConstants;
import com.flourishtravel.domain.flora.dto.FloraSuggestedActionDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Component
public class FloraNearbyChatbotHelper {

    public ChatbotResponse maybeEnhance(ChatbotRequest request, ChatbotResponse response, UUID userId) {
        if (userId == null || request.getBookingId() == null) return response;
        String content = resolveContent(request);
        if (!looksLikeNearbyIntent(content)) return response;

        List<FloraSuggestedActionDto> actions = new ArrayList<>();
        if (response.getSuggestedActions() != null) {
            actions.addAll(response.getSuggestedActions());
        }
        actions.add(FloraSuggestedActionDto.builder()
                .type(FloraRecommendationConstants.ACTION_OPEN_NEARBY)
                .label("Xem gợi ý gần đây")
                .payload(request.getBookingId().toString())
                .build());

        String reply = response.getReply();
        if (reply == null || reply.isBlank()) {
            reply = "Flora có thể gợi ý địa điểm gần bạn theo thời gian còn lại trong lịch trình. Bạn bấm nút bên dưới để xem nhé.";
        }

        return response.toBuilder()
                .reply(reply)
                .answer(reply)
                .suggestedActions(actions)
                .build();
    }

    private static boolean looksLikeNearbyIntent(String content) {
        if (content == null || content.isBlank()) return false;
        String lower = content.toLowerCase(Locale.ROOT);
        return lower.contains("gợi ý quán")
                || lower.contains("quán ăn gần")
                || lower.contains("gần đây có gì")
                || lower.contains("gần đây có")
                || lower.contains("đủ thời gian")
                || (lower.contains("đi đâu") && (lower.contains("còn") || lower.contains("thời gian")))
                || lower.contains("cafe gần")
                || lower.contains("cà phê gần");
    }

    private static String resolveContent(ChatbotRequest request) {
        if (request.getContent() != null && !request.getContent().isBlank()) return request.getContent().trim();
        if (request.getMessage() != null && !request.getMessage().isBlank()) return request.getMessage().trim();
        return "";
    }
}
