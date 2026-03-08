package com.flourishtravel.domain.chatbot.service;

import com.flourishtravel.domain.chatbot.dto.ChatbotRequest;
import com.flourishtravel.domain.chatbot.dto.ChatbotResponse;
import com.flourishtravel.domain.chatbot.entity.PolicyFaq;
import com.flourishtravel.domain.chatbot.entity.SearchLog;
import com.flourishtravel.domain.chatbot.repository.PolicyFaqRepository;
import com.flourishtravel.domain.chatbot.repository.SearchLogRepository;
import com.flourishtravel.domain.tour.entity.Tour;
import com.flourishtravel.domain.tour.entity.TourImage;
import com.flourishtravel.domain.tour.repository.TourRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotService {

    private final TourRepository tourRepository;
    private final LlmService llmService;
    private final PolicyFaqRepository policyFaqRepository;
    private final SearchLogRepository searchLogRepository;

    private static final String PROMPT_TEMPLATE = """
Bạn là trợ lý tư vấn tour du lịch. User vừa nói: "%s"
Trả lời ĐÚNG 1 JSON (không markdown, không giải thích) theo form:
{"intent":"search_tour|general_question|travel_tips|policy_faq|unknown","slots":{"destination": "tên địa điểm hoặc null","duration_days": số ngày hoặc null,"budget_min": số triệu VND hoặc null,"budget_max": số triệu VND hoặc null,"guest_count": số người hoặc null},"reply":"1-2 câu tiếng Việt thân thiện","quick_replies":[{"label":"Nút 1","payload":"text"},{"label":"Nút 2","payload":"text"}]}
- intent search_tour: user muốn tìm tour. Nếu thiếu thông tin thì reply hỏi và quick_replies gợi ý.
- general_question: hỏi chung (ăn gì, thời tiết...). reply trả lời ngắn.
- policy_faq: hỏi về chính sách hủy tour, đổi ngày, hoàn tiền, thanh toán, trẻ em. reply tóm tắt ngắn.
- unknown: không rõ. reply xin lỗi, gợi ý "Để lại thông tin tư vấn".
""";

    @Transactional
    public ChatbotResponse processMessage(ChatbotRequest request) {
        try {
            String content = request.getContent() != null ? request.getContent().trim() : "";
            if (content.isEmpty()) {
                return ChatbotResponse.builder()
                        .reply("Bạn có thể nhập ví dụ: 'Tour biển 3 ngày tầm 5 triệu' hoặc 'Chính sách hủy tour?' để mình hỗ trợ.")
                        .quickReplies(List.of(
                                ChatbotResponse.QuickReply.builder().label("Tour biển 3 ngày").payload("Tour biển 3 ngày").build(),
                                ChatbotResponse.QuickReply.builder().label("Chính sách hủy tour").payload("Chính sách hủy tour").build()
                        ))
                        .build();
            }

            Map<String, Object> llmJson = llmService.generateJson(String.format(PROMPT_TEMPLATE, content));
            if (llmJson != null) {
                return buildResponseFromLlm(llmJson, content, request);
            }

            return fallbackResponse(content);
        } catch (Exception e) {
            log.error("Chatbot processMessage failed", e);
            return ChatbotResponse.builder()
                    .reply("Mình đang gặp chút sự cố kỹ thuật. Bạn thử nhập lại hoặc chọn nút bên dưới nhé.")
                    .quickReplies(List.of(
                            ChatbotResponse.QuickReply.builder().label("Tour biển 3 ngày").payload("Tour biển 3 ngày").build(),
                            ChatbotResponse.QuickReply.builder().label("Tìm tour Đà Nẵng").payload("Tìm tour Đà Nẵng").build()
                    ))
                    .build();
        }
    }

    @SuppressWarnings("unchecked")
    private ChatbotResponse buildResponseFromLlm(Map<String, Object> llmJson, String content, ChatbotRequest request) {
        String intent = getString(llmJson, "intent");
        String reply = getString(llmJson, "reply");
        if (reply == null || reply.isBlank()) reply = "Mình đã ghi nhận, bạn cần thêm thông tin gì không?";

        if ("policy_faq".equals(intent)) {
            String policyReply = getPolicyReply(content);
            if (policyReply != null) reply = policyReply;
        }

        List<ChatbotResponse.QuickReply> quickReplies = new ArrayList<>();
        Object qr = llmJson.get("quick_replies");
        if (qr instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> m) {
                    String label = getString((Map<String, Object>) m, "label");
                    String payload = getString((Map<String, Object>) m, "payload");
                    if (label != null) quickReplies.add(ChatbotResponse.QuickReply.builder().label(label).payload(payload != null ? payload : label).build());
                }
            }
        }
        if (quickReplies.isEmpty()) {
            quickReplies.add(ChatbotResponse.QuickReply.builder().label("Xem thêm tour").payload("Xem thêm tour").build());
        }
        if ("unknown".equals(intent)) {
            quickReplies.add(0, ChatbotResponse.QuickReply.builder().label("Để lại thông tin tư vấn").payload("Để lại thông tin tư vấn").build());
        }

        Map<String, Object> slots = new HashMap<>();
        Object slotsObj = llmJson.get("slots");
        if (slotsObj instanceof Map<?, ?> m) {
            for (Map.Entry<?, ?> e : ((Map<?, ?>) slotsObj).entrySet()) {
                if (e.getValue() != null && !"null".equals(String.valueOf(e.getValue()))) {
                    slots.put(String.valueOf(e.getKey()), e.getValue());
                }
            }
        }

        Map<String, Object> state = new HashMap<>();
        state.put("intent", intent != null ? intent : "unknown");
        state.put("slots", slots);

        String destination = slots.get("destination") != null ? String.valueOf(slots.get("destination")) : null;
        BigDecimal minPrice = null;
        BigDecimal maxPrice = null;
        if (slots.get("budget_min") instanceof Number n) minPrice = BigDecimal.valueOf(n.doubleValue() * 1_000_000);
        if (slots.get("budget_max") instanceof Number n) maxPrice = BigDecimal.valueOf(n.doubleValue() * 1_000_000);
        Integer durationDays = slots.get("duration_days") instanceof Number n ? n.intValue() : null;

        List<ChatbotResponse.TourCard> tours = List.of();
        if ("search_tour".equals(intent)) {
            if (destination != null && !destination.isBlank() || minPrice != null || maxPrice != null) {
                var page = tourRepository.search(
                        destination != null && !destination.isBlank() ? destination : null,
                        minPrice, maxPrice, null, null,
                        PageRequest.of(0, 6));
                tours = page.getContent().stream().map(this::toTourCard).collect(Collectors.toList());
                saveSearchLog(request, content, destination, minPrice, maxPrice, durationDays, tours.size());
            } else {
                var page = tourRepository.search(null, null, null, null, null, PageRequest.of(0, 6));
                tours = page.getContent().stream().map(this::toTourCard).collect(Collectors.toList());
                saveSearchLog(request, content, null, null, null, null, tours.size());
            }
        }

        return ChatbotResponse.builder()
                .reply(reply)
                .tours(tours)
                .quickReplies(quickReplies)
                .state(state)
                .build();
    }

    private String getPolicyReply(String userMessage) {
        if (userMessage == null) return null;
        String lower = userMessage.toLowerCase();
        List<PolicyFaq> all = policyFaqRepository.findAllByOrderBySortOrderAsc();
        if (all.isEmpty()) return null;
        if (lower.contains("hủy") || lower.contains("cancel")) return all.stream().filter(p -> "cancellation".equals(p.getTopicKey())).findFirst().map(PolicyFaq::getContent).orElse(all.get(0).getContent());
        if (lower.contains("đổi ngày") || lower.contains("đổi tour")) return all.stream().filter(p -> "change_date".equals(p.getTopicKey())).findFirst().map(PolicyFaq::getContent).orElse(all.get(0).getContent());
        if (lower.contains("hoàn tiền") || lower.contains("refund")) return all.stream().filter(p -> "refund".equals(p.getTopicKey())).findFirst().map(PolicyFaq::getContent).orElse(all.get(0).getContent());
        if (lower.contains("thanh toán") || lower.contains("payment")) return all.stream().filter(p -> "payment".equals(p.getTopicKey())).findFirst().map(PolicyFaq::getContent).orElse(all.get(0).getContent());
        if (lower.contains("trẻ em") || lower.contains("trẻ con") || lower.contains("trẻ nhỏ")) return all.stream().filter(p -> "children".equals(p.getTopicKey())).findFirst().map(PolicyFaq::getContent).orElse(all.get(0).getContent());
        if (lower.contains("chính sách")) return all.get(0).getContent();
        return all.get(0).getContent();
    }

    private void saveSearchLog(ChatbotRequest request, String query, String destination, BigDecimal minPrice, BigDecimal maxPrice, Integer durationDays, int resultCount) {
        try {
            SearchLog entry = SearchLog.builder()
                    .user(null)
                    .sessionId(request.getSessionId())
                    .searchQuery(query)
                    .destination(destination)
                    .minPrice(minPrice)
                    .maxPrice(maxPrice)
                    .durationDays(durationDays)
                    .resultCount(resultCount)
                    .build();
            searchLogRepository.save(entry);
        } catch (Exception e) {
            this.log.warn("Failed to save search log", e);
        }
    }

    private String getString(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v != null ? v.toString().trim() : null;
    }

    private ChatbotResponse.TourCard toTourCard(Tour t) {
        String imageUrl = null;
        if (t.getImages() != null && !t.getImages().isEmpty()) {
            TourImage first = t.getImages().get(0);
            if (first != null) imageUrl = first.getImageUrl();
        }
        return ChatbotResponse.TourCard.builder()
                .id(t.getId().toString())
                .title(t.getTitle())
                .slug(t.getSlug())
                .price(t.getBasePrice() != null ? t.getBasePrice().longValue() : 0L)
                .durationDays(t.getDurationDays())
                .imageUrl(imageUrl)
                .build();
    }

    private ChatbotResponse fallbackResponse(String content) {
        String keyword = extractSearchKeyword(content);
        var page = tourRepository.search(
                keyword,
                null, null, null, null,
                PageRequest.of(0, 8));
        List<ChatbotResponse.TourCard> cards = page.getContent().stream().map(this::toTourCard).collect(Collectors.toList());
        String reply = cards.isEmpty()
                ? "Hiện chưa có tour nào khớp với yêu cầu của bạn. Bạn thử gợi ý bên dưới hoặc để lại thông tin để mình tư vấn nhé."
                : "Dựa trên yêu cầu của bạn, đây là một số tour phù hợp. Bạn có thể xem chi tiết hoặc thử tìm theo địa điểm/ số ngày khác nhé.";
        return ChatbotResponse.builder()
                .reply(reply)
                .tours(cards)
                .quickReplies(List.of(
                        ChatbotResponse.QuickReply.builder().label("Tour biển 3 ngày").payload("Tour biển 3 ngày").build(),
                        ChatbotResponse.QuickReply.builder().label("Tour Đà Lạt").payload("Tour Đà Lạt").build(),
                        ChatbotResponse.QuickReply.builder().label("Tour Phan Thiết").payload("Tour Phan Thiết").build()
                ))
                .build();
    }

    /** Rút từ khóa tìm tour từ nội dung user (fallback khi không có LLM). */
    private String extractSearchKeyword(String content) {
        if (content == null || content.isBlank()) return null;
        String lower = content.toLowerCase().trim();
        if (lower.contains("đà nẵng") || lower.contains("da nang")) return "Đà Nẵng";
        if (lower.contains("đà lạt") || lower.contains("da lat")) return "Đà Lạt";
        if (lower.contains("phan thiết") || lower.contains("mũi né") || lower.contains("mui ne")) return "Phan Thiết";
        if (lower.contains("nha trang")) return "Nha Trang";
        if (lower.contains("phú quốc") || lower.contains("phu quoc")) return "Phú Quốc";
        if (lower.contains("hạ long") || lower.contains("ha long")) return "Hạ Long";
        if (lower.contains("hội an") || lower.contains("hoi an")) return "Hội An";
        if (lower.contains("vũng tàu") || lower.contains("vung tau")) return "Vũng Tàu";
        if (lower.contains("ninh bình") || lower.contains("ninh binh") || lower.contains("tam cốc")) return "Ninh Bình";
        if (lower.contains("huế") || lower.contains("hue")) return "Huế";
        if (lower.contains("côn đảo") || lower.contains("con dao")) return "Côn Đảo";
        if (lower.contains("quy nhơn") || lower.contains("quy nhon")) return "Quy Nhơn";
        if (lower.contains("sapa") || lower.contains("sa pa")) return "Sapa";
        if (lower.contains("cần thơ") || lower.contains("can tho") || lower.contains("châu đốc")) return "Cần Thơ";
        if (lower.contains("biển") || lower.contains("bãi") || lower.contains("tour biển")) return "biển";
        if (lower.contains("tour") && content.length() > 3) return "tour";
        return content.length() > 2 ? content : null;
    }
}
