package com.flourishtravel.domain.flora;

import com.flourishtravel.domain.chatbot.dto.ChatbotResponse;

import java.util.List;

public final class FloraQuickActions {

    private FloraQuickActions() {}

    public static List<ChatbotResponse.QuickReply> defaults() {
        return List.of(
                qr("Gợi ý tour cho tôi", "Gợi ý tour cho tôi"),
                qr("Gần đây có gì tham quan?", "Gần đây có gì tham quan?"),
                qr("Còn bao lâu lên xe?", "Còn bao lâu lên xe?"),
                qr("Gợi ý quán ăn gần đây", "Gợi ý quán ăn gần đây"),
                qr("Tôi đang ở đâu trong lịch trình?", "Tôi đang ở đâu trong lịch trình?"),
                qr("Chỉ đường về điểm tập trung", "Chỉ đường về điểm tập trung"),
                qr("Gợi ý lịch trình theo ngân sách", "Gợi ý lịch trình theo ngân sách")
        );
    }

    private static ChatbotResponse.QuickReply qr(String label, String payload) {
        return ChatbotResponse.QuickReply.builder().label(label).payload(payload).build();
    }
}
