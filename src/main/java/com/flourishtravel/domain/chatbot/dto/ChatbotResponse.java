package com.flourishtravel.domain.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotResponse {

    private String reply;
    private List<TourCard> tours;
    private List<QuickReply> quickReplies;
    private Map<String, Object> state;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TourCard {
        private String id;
        private String title;
        private String slug;
        private Long price;
        private Integer durationDays;
        private String imageUrl;
        /** Nút chú thích: Lịch trình, Địa điểm, Giá cả – bấm vào gửi payload để BE trả data từ DB */
        private List<QuickReply> actions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuickReply {
        private String label;
        private String payload;
    }
}
