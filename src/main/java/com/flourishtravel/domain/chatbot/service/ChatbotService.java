package com.flourishtravel.domain.chatbot.service;

import com.flourishtravel.domain.chatbot.dto.ChatbotRequest;
import com.flourishtravel.domain.chatbot.dto.ChatbotResponse;
import com.flourishtravel.domain.chatbot.entity.ChatbotIntent;
import com.flourishtravel.domain.chatbot.entity.ChatbotTrainingPhrase;
import com.flourishtravel.domain.chatbot.entity.PolicyFaq;
import com.flourishtravel.domain.chatbot.entity.SearchLog;
import com.flourishtravel.domain.chatbot.repository.ChatbotTrainingPhraseRepository;
import com.flourishtravel.domain.chatbot.repository.PolicyFaqRepository;
import com.flourishtravel.domain.chatbot.repository.SearchLogRepository;
import com.flourishtravel.domain.chatbot.service.ChatbotConfigService.ChatbotIntentWithPhrases;
import com.flourishtravel.domain.tour.dto.AvailabilityCheckDto;
import com.flourishtravel.domain.tour.entity.Tour;
import com.flourishtravel.domain.tour.entity.TourImage;
import com.flourishtravel.domain.tour.entity.TourItinerary;
import com.flourishtravel.domain.tour.entity.TourLocation;
import com.flourishtravel.domain.tour.repository.TourRepository;
import org.hibernate.Hibernate;
import com.flourishtravel.domain.tour.service.TourService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotService {

    private final TourRepository tourRepository;
    private final LlmService llmService;
    private final PolicyFaqRepository policyFaqRepository;
    private final ChatbotTrainingPhraseRepository trainingPhraseRepository;
    private final SearchLogRepository searchLogRepository;
    private final ChatbotConfigService chatbotConfigService;
    private final ObjectMapper objectMapper;
    private final TourService tourService;
    private final ChatbotDataService chatbotDataService;

    private static final String PROMPT_TEMPLATE = """
Persona: Bạn là chuyên viên tư vấn du lịch của FlourishTravel – lịch sự, chuyên nghiệp, nhiệt tình nhưng không quá suồng sã. Trả lời ngắn gọn, rõ ràng; khi cần chuyển sang nhân viên thì hướng dẫn cụ thể (hotline, form Liên hệ). CHỈ tư vấn trong lĩnh vực tour du lịch/chính sách. Nếu user viết tiếng Anh/Trung/Hàn: trả lời CÙNG ngôn ngữ đó.

User vừa nói: "%s"
%s
Trả lời ĐÚNG 1 JSON (không markdown, không giải thích):
{"intent":"search_tour|general_question|travel_tips|policy_faq|trip_planner|unknown","slots":{"destination": "tên địa điểm hoặc null","duration_days": số ngày hoặc null,"budget_min": số triệu VND hoặc null,"budget_max": số triệu VND hoặc null,"guest_count": số người hoặc null,"destinations": ["địa điểm 1","địa điểm 2"] hoặc null},"reply":"1-2 câu theo persona (ngôn ngữ theo user)","quick_replies":[{"label":"...","payload":"..."}]}

Bộ nhớ ngữ cảnh (Context Memory): Nếu context từ lượt trước có guest_count, destination, duration_days hoặc khách từng nói "đi với con nhỏ"/"2 con"/"gia đình" thì các câu sau BẠN TỰ GỢI Ý tour/điểm có khu vui chơi, thực đơn không cay, phù hợp gia đình mà KHÔNG cần khách nhắc lại. Dùng slots đã có để reply nhất quán.

Đa ý định (Multi-intent): Nếu khách hỏi 2 việc trong 1 câu (vd "Tour này có đi Hội An không và ở lại thêm 1 đêm tính phí thế nào?") thì reply phải BÓC TÁCH và trả lời CẢ HAI ý: (1) có đi Hội An hay không theo lịch trình, (2) ở lại thêm đêm thì phí theo chính sách/option. Không bỏ sót ý.

Entity: Nhặt từ câu: địa điểm -> destination, số ngày -> duration_days, ngân sách (X triệu) -> budget_min/budget_max, số người -> guest_count. Slang: đl=Đà Lạt, ks=khách sạn, mb=miền Bắc, mt=miền Trung, mn=miền Nam.

Các intent:
- search_tour: tìm/đặt/xem tour; tư vấn theo ngân sách/phong cách/thời tiết. Điền slots; reply gợi ý tour, có thể nhắc "để lại SĐT/email nhận lịch trình".
- trip_planner: có N ngày muốn đi X, Y. slots: duration_days, destination hoặc destinations.
- general_question: hỏi chung du lịch (ăn gì, thời tiết). Local secrets (quán ẩn, góc chụp, trả giá, taxi) -> policy_faq hoặc general_question tùy câu.
- travel_tips: mẹo du lịch, hành trang.
- policy_faq: hỏi cần FAQ chính xác, gồm: lịch trình/so sánh/thời điểm/độ tuổi; giá/thanh toán/trẻ em; hủy/đổi/hoàn; in-tour/khẩn cấp; visa/bảo hiểm; dịch vụ thêm; hành trang; văn hóa/đổi tiền/ổ cắm; XỬ LÝ SỰ CỐ (bão/mưa tour khởi hành không, đền bù; say sóng/ngộ độc/cấp cứu; mất ví/hộ chiếu trình báo công an); LOCAL SECRETS (quán ẩn, góc check-in, trả giá, taxi); TIỆN ÍCH SỐ (quy đổi USD/Baht, dự báo thời tiết, dịch câu tiếng Thái); SAU CHUYẾN (review/voucher, khiếu nại HDV, gợi ý tour tiếp); SO SÁNH ĐỐI THỦ (bên X rẻ hơn, khách sạn xa); UY TÍN (hoạt động bao lâu, giấy phép) -> policy_faq.
- unknown: không liên quan tour. reply lịch sự + quick_replies "Tour biển 3 ngày", "Chính sách hủy tour", "Để lại thông tin tư vấn".

Upselling/Chốt đơn (khi hợp): Gợi ý xe đưa đón, vé show, SIM; "còn ít chỗ", "đặt trong chat giảm 5%%", "để lại SĐT/email nhận khuyến mãi". Không spam.
""";

    @Transactional
    public ChatbotResponse processMessage(ChatbotRequest request) {
        try {
            String raw = request.getContent() != null ? request.getContent().trim() : "";
            String content = normalizeUserInput(raw);
            if (content.isEmpty()) {
                return ChatbotResponse.builder()
                        .reply("Bạn có thể nhập ví dụ: 'Tour biển 3 ngày tầm 5 triệu' hoặc 'Chính sách hủy tour?' để mình hỗ trợ.")
                        .quickReplies(List.of(
                                ChatbotResponse.QuickReply.builder().label("Tour biển 3 ngày").payload("Tour biển 3 ngày").build(),
                                ChatbotResponse.QuickReply.builder().label("Chính sách hủy tour").payload("Chính sách hủy tour").build()
                        ))
                        .build();
            }

            if (looksLikeNegativeSentiment(content)) {
                return buildSentimentResponse();
            }

            // Chú thích tour: user bấm "Lịch trình" / "Địa điểm" / "Giá cả" trên card → trả data từ DB
            ChatbotResponse tourDetailResponse = buildResponseForTourDetailAction(content);
            if (tourDetailResponse != null) return tourDetailResponse;

            // Ưu tiên match intent từ training phrases (config import); có state thì ưu tiên intent theo context câu trước
            ChatbotIntentWithPhrases matched = matchIntentFromTrainingPhrases(content, request);
            if (matched != null) {
                ChatbotResponse intentResponse = buildResponseFromIntent(matched, content, request);
                if (intentResponse != null) return intentResponse;
            }

            String contentForPolicy = normalizeForMatching(content);
            if (looksLikePolicyQuestion(contentForPolicy)) {
                String policyReply = getPolicyReply(content);
                if (policyReply != null) {
                    // Câu hỏi thời điểm / nên đi đâu có địa điểm → tra DB tour, có thì hiện tour + chú thích, không thì báo và gợi ý tour khác
                    if (isBestTimeOrGeneralPlaceQuestion(content)) {
                        ChatbotResponse bestTimeResponse = buildBestTimeOrGeneralPlaceResponse(policyReply, content, request);
                        if (bestTimeResponse != null) return bestTimeResponse;
                    }
                    return buildPolicyOnlyResponse(policyReply, looksLikeHumanHandoff(content));
                }
            }

            String contextHint = buildContextHint(request.getState());
            String contentForFormat = content.replace("%", "%%");
            String hintForFormat = contextHint.replace("%", "%%");
            Map<String, Object> llmJson = llmService.generateJson(String.format(PROMPT_TEMPLATE, contentForFormat, hintForFormat));
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

    /** Chuẩn hóa tiếng lóng/viết tắt để AI và từ khóa hiểu đúng. */
    private static String normalizeUserInput(String content) {
        if (content == null || content.isBlank()) return content;
        String s = content.trim();
        s = s.replaceAll("(?i)\\bđl\\b", "Đà Lạt");
        s = s.replaceAll("(?i)\\bks\\b", "khách sạn");
        s = s.replaceAll("(?i)\\bmb\\b", "miền Bắc");
        s = s.replaceAll("(?i)\\bmt\\b", "miền Trung");
        s = s.replaceAll("(?i)\\bmn\\b", "miền Nam");
        return s;
    }

    private static boolean looksLikeNegativeSentiment(String content) {
        if (content == null || content.isBlank()) return false;
        String lower = content.toLowerCase();
        return lower.contains("đắt quá") || lower.contains("rẻ hơn") && (lower.contains("bên khác") || lower.contains("tận ") || lower.contains("1 triệu"))
                || lower.contains("giảm thêm") || lower.contains("có giảm không") && lower.contains("đắt")
                || lower.contains("cũ lắm") || lower.contains("cũ rồi") || lower.contains("nghe nói") && (lower.contains("cũ") || lower.contains("kém"))
                || lower.contains("không đảm bảo") || lower.contains("vệ sinh") && (lower.contains("đồ ăn") || lower.contains("có đảm bảo"))
                || lower.contains("sao bên khác") || lower.contains("bán rẻ hơn");
    }

    private ChatbotResponse buildSentimentResponse() {
        String reply = findByTopic(policyFaqRepository.findAllByOrderBySortOrderAsc(), "sentiment_apology");
        if (reply == null) reply = "FlourishTravel rất tiếc nếu trải nghiệm chưa như mong đợi. Bạn vui lòng gọi hotline hoặc để lại tin nhắn qua form Liên hệ để nhân viên hỗ trợ cụ thể nhé.";
        return ChatbotResponse.builder()
                .reply(reply)
                .tours(List.of())
                .quickReplies(List.of(
                        ChatbotResponse.QuickReply.builder().label("Liên hệ nhân viên").payload("Liên hệ nhân viên").build(),
                        ChatbotResponse.QuickReply.builder().label("Chính sách hủy/đổi").payload("Chính sách hủy tour").build(),
                        ChatbotResponse.QuickReply.builder().label("Xem tour khác").payload("Xem thêm tour").build()
                ))
                .build();
    }

    private String buildContextHint(Map<String, Object> previousState) {
        if (previousState == null) return "";
        Object slotsObj = previousState.get("slots");
        if (!(slotsObj instanceof Map<?, ?> m) || m.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("Context từ lượt trước (đã biết): ");
        ((Map<?, ?>) slotsObj).forEach((k, v) -> {
            if (v != null && !"null".equals(String.valueOf(v))) sb.append(k).append("=").append(v).append("; ");
        });
        return sb.append("\n").toString();
    }

    @SuppressWarnings("unchecked")
    private ChatbotResponse buildResponseFromLlm(Map<String, Object> llmJson, String content, ChatbotRequest request) {
        String intent = getString(llmJson, "intent");
        String reply = getString(llmJson, "reply");
        if (reply == null || reply.isBlank()) reply = "Mình đã ghi nhận, bạn cần thêm thông tin gì không?";

        Map<String, Object> slots = new HashMap<>();
        Object prevSlotsObj = request.getState() != null ? request.getState().get("slots") : null;
        if (prevSlotsObj instanceof Map<?, ?> prev) {
            prev.forEach((k, v) -> {
                if (v != null && !"null".equals(String.valueOf(v))) slots.put(String.valueOf(k), v);
            });
        }
        Object slotsObj = llmJson.get("slots");
        if (slotsObj instanceof Map<?, ?> m) {
            for (Map.Entry<?, ?> e : ((Map<?, ?>) slotsObj).entrySet()) {
                if (e.getValue() != null && !"null".equals(String.valueOf(e.getValue()))) {
                    slots.put(String.valueOf(e.getKey()), e.getValue());
                }
            }
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

        if ("policy_faq".equals(intent)) {
            String policyReply = getPolicyReply(content);
            if (policyReply != null) {
                reply = policyReply;
                if (looksLikeHumanHandoff(content)) {
                    quickReplies.add(0, ChatbotResponse.QuickReply.builder().label("Liên hệ nhân viên").payload("Liên hệ nhân viên").build());
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
            tours = searchToursWithDuration(destination, minPrice, maxPrice, durationDays, 6);
            saveSearchLog(request, content, destination, minPrice, maxPrice, durationDays, tours.size());
        } else if ("trip_planner".equals(intent)) {
            String destForSearch = destination;
            if (destForSearch == null || destForSearch.isBlank()) {
                Object dests = slots.get("destinations");
                if (dests instanceof List<?> list && !list.isEmpty()) destForSearch = String.valueOf(list.get(0));
            }
            tours = searchToursWithDuration(destForSearch, null, null, durationDays, 6);
            if (reply == null || reply.isBlank()) reply = "Dựa trên số ngày và điểm đến của bạn, đây là một số tour gợi ý. Bạn có thể kết hợp nhiều tour hoặc chọn một tour trọn gói.";
        } else if ("general_question".equals(intent) || "travel_tips".equals(intent)) {
            String keyword = extractSearchKeyword(content);
            if (keyword != null) {
                List<ChatbotResponse.TourCard> related = searchToursWithDuration(keyword, null, null, null, 2);
                if (!related.isEmpty()) tours = related;
            }
        }
        if (!tours.isEmpty() && tours.get(0).getSlug() != null) {
            state.put("lastSuggestedTourSlug", tours.get(0).getSlug());
        }

        return ChatbotResponse.builder()
                .reply(reply)
                .tours(tours)
                .quickReplies(quickReplies)
                .state(state)
                .build();
    }

    private List<Tour> searchTourEntities(String destination, BigDecimal minPrice, BigDecimal maxPrice, Integer durationDays, int limit) {
        String destinationPattern = (destination != null && !destination.isBlank())
                ? "%" + destination.trim() + "%"
                : null;
        var page = tourRepository.searchForSuggestion(
                destinationPattern,
                minPrice, maxPrice, null,
                PageRequest.of(0, limit * 2));
        List<Tour> list = page.getContent();
        if (durationDays != null && durationDays > 0) {
            list = list.stream()
                    .filter(t -> t.getDurationDays() != null && t.getDurationDays().equals(durationDays))
                    .limit(limit)
                    .toList();
            if (list.isEmpty()) {
                list = page.getContent().stream()
                        .filter(t -> t.getDurationDays() != null && Math.abs(t.getDurationDays() - durationDays) <= 1)
                        .limit(limit)
                        .toList();
            }
        }
        return list.stream().limit(limit).toList();
    }

    private List<ChatbotResponse.TourCard> searchToursWithDuration(String destination, BigDecimal minPrice, BigDecimal maxPrice, Integer durationDays, int limit) {
        List<Tour> list = searchTourEntities(destination, minPrice, maxPrice, durationDays, limit);
        return list.stream().map(this::toTourCard).collect(Collectors.toList());
    }

    private static boolean looksLikePolicyQuestion(String content) {
        if (content == null || content.isBlank()) return false;
        String lower = content.toLowerCase();
        return lower.contains("chính sách") || lower.contains("hủy") || lower.contains("cancel")
                || lower.contains("đổi ngày") || lower.contains("đổi tour") || lower.contains("đổi lịch") || lower.contains("dời ngày") || lower.contains("đổi tên")
                || lower.contains("hoàn tiền") || lower.contains("hoàn lại") || lower.contains("refund") || lower.contains("trả lại tiền")
                || lower.contains("thanh toán") || lower.contains("payment") || lower.contains("đặt cọc") || lower.contains("trả tiền") || lower.contains("chuyển khoản") || lower.contains("trả góp") || lower.contains("thẻ tín dụng")
                || lower.contains("trẻ em") || lower.contains("trẻ con") || lower.contains("trẻ nhỏ") || lower.contains("con nhỏ") || lower.contains("đi cùng con") || lower.contains("giường riêng") || lower.contains("bé ") || lower.contains(" bé ")
                || lower.contains("lịch trình") || lower.contains("ngày 2") || lower.contains("ngày 3") || lower.contains("thời gian tự do") || (lower.contains("đi đâu") && lower.contains("ngày"))
                || lower.contains("so sánh gói") || lower.contains("standard") && lower.contains("pro") || lower.contains("đắt hơn") || lower.contains("khác nhau chỗ nào")
                || (lower.contains("tháng ") && (lower.contains("đi ") || lower.contains("đẹp") || lower.contains("mưa"))) || lower.contains("mùa nào") || lower.contains("ít mưa")
                || lower.contains("người già") || lower.contains("vất vả") || lower.contains("ăn chay") || lower.contains("thực đơn")
                || lower.contains("giá đã") || lower.contains("bao gồm") || lower.contains("vé máy bay") || lower.contains("tip") || lower.contains("chi phí ẩn")
                || lower.contains("đổi phòng") || lower.contains("điều hòa") || lower.contains("hướng dẫn viên") || lower.contains("hdv") || lower.contains("điểm hẹn") || lower.contains("quên ") || lower.contains("để quên") || lower.contains("thất lạc") || lower.contains("hotline") || lower.contains("khẩn cấp")
                || lower.contains("visa") || lower.contains("hộ chiếu") || lower.contains("passport") || lower.contains("còn hạn")
                || lower.contains("bảo hiểm") || lower.contains("bồi thường") || lower.contains("tai nạn") || lower.contains("nước ngoài")
                || lower.contains("liên hệ nhân viên") || lower.contains("gặp người") || lower.contains("chuyển người") || lower.contains("tư vấn viên")
                || lower.contains("xe đưa đón") || lower.contains("vé show") || lower.contains("ký ức hội an") || lower.contains("sim ") || lower.contains("sim 4g") || lower.contains("dịch vụ thêm")
                || lower.contains("mang theo") || lower.contains("hành trang") || lower.contains("áo phao") || lower.contains("giữ nhiệt") || lower.contains("fansipan") && lower.contains("lạnh")
                || lower.contains("singapore") && (lower.contains("kẹo") || lower.contains("cao su")) || lower.contains("thái lan") && (lower.contains("váy") || lower.contains("đền") || lower.contains("chùa"))
                || lower.contains("đổi tiền") || lower.contains("tỉ giá") || lower.contains("won") || lower.contains("đổi ngoại tệ")
                || lower.contains("ổ cắm") || lower.contains("2 chấu") || lower.contains("3 chấu") || lower.contains("cục chuyển")
                || lower.contains("bão") && (lower.contains("tour") || lower.contains("khởi hành") || lower.contains("phú quốc"))
                || lower.contains("mưa lớn") || lower.contains("đền bù") && (lower.contains("thời tiết") || lower.contains("mưa"))
                || lower.contains("say sóng") || lower.contains("đường bộ") && lower.contains("đổi")
                || lower.contains("ngộ độc") || lower.contains("cấp cứu") || lower.contains("gọi ") && (lower.contains("cấp cứu") || lower.contains("115"))
                || lower.contains("mất ví") || lower.contains("mất hộ chiếu") || lower.contains("trình báo công an") || lower.contains("trình báo ở đâu")
                || lower.contains("quán ẩn") || lower.contains("ốc ngon") || lower.contains("dân địa phương") || lower.contains("góc check-in") || lower.contains("bình minh") && (lower.contains("đà lạt") || lower.contains("chụp"))
                || lower.contains("trả giá") || lower.contains("chặt chém") || lower.contains("taxi") && (lower.contains("chặt") || lower.contains("đắt"))
                || lower.contains("usd") && (lower.contains("baht") || lower.contains("đổi ra") || lower.contains("bao nhiêu")) || lower.contains("tỉ giá hôm nay")
                || lower.contains("dự báo thời tiết") || lower.contains("thời tiết") && (lower.contains("3 ngày") || lower.contains("theo giờ"))
                || lower.contains("dịch") && (lower.contains("tiếng thái") || lower.contains("phiên âm") || lower.contains("nói bằng"))
                || lower.contains("chuyến đi vừa rồi") || lower.contains("review") || lower.contains("chia sẻ ảnh") || lower.contains("voucher") && lower.contains("lần sau")
                || lower.contains("không hài lòng") || lower.contains("thái độ") && lower.contains("hdv") || lower.contains("phản ánh") || lower.contains("khiếu nại")
                || lower.contains("gợi ý lần sau") || lower.contains("lễ hội") && (lower.contains("nha trang") || lower.contains("tháng sau"))
                || lower.contains("bên ") && (lower.contains("rẻ hơn") || lower.contains("đặc biệt")) || lower.contains("tại sao") && (lower.contains("đắt") || lower.contains("xa trung tâm"))
                || lower.contains("khách sạn") && lower.contains("xa") || lower.contains("hoạt động bao lâu") || lower.contains("giấy phép kinh doanh")
                || (lower.contains("điều khoản") || (lower.contains("được không") && (lower.contains("đổi") || lower.contains("hủy") || lower.contains("hoàn"))));
    }

    private static boolean looksLikeHumanHandoff(String content) {
        if (content == null || content.isBlank()) return false;
        String lower = content.toLowerCase();
        return lower.contains("khẩn cấp") || lower.contains("liên hệ nhân viên") || lower.contains("gặp người") || lower.contains("chuyển người") || lower.contains("tư vấn viên");
    }

    private ChatbotResponse buildPolicyOnlyResponse(String policyReply, boolean addHumanHandoff) {
        List<ChatbotResponse.QuickReply> qr = new ArrayList<>();
        if (addHumanHandoff) {
            qr.add(ChatbotResponse.QuickReply.builder().label("Liên hệ nhân viên").payload("Liên hệ nhân viên").build());
        }
        qr.add(ChatbotResponse.QuickReply.builder().label("Chính sách đổi ngày").payload("Chính sách đổi ngày").build());
        qr.add(ChatbotResponse.QuickReply.builder().label("Hoàn tiền").payload("Hoàn tiền").build());
        qr.add(ChatbotResponse.QuickReply.builder().label("Tour biển 3 ngày").payload("Tour biển 3 ngày").build());
        return ChatbotResponse.builder()
                .reply(policyReply)
                .tours(List.of())
                .quickReplies(qr)
                .build();
    }

    /** User bấm chú thích trên card: "xem_lich_trinh:slug", "xem_dia_diem:slug", "xem_gia:slug" → trả data từ DB. */
    private ChatbotResponse buildResponseForTourDetailAction(String content) {
        if (content == null || content.length() < 10) return null;
        String trim = content.trim();
        String slug = null;
        String action = null;
        if (trim.startsWith("xem_lich_trinh:")) {
            action = "lich_trinh";
            slug = trim.substring("xem_lich_trinh:".length()).trim();
        } else if (trim.startsWith("xem_dia_diem:")) {
            action = "dia_diem";
            slug = trim.substring("xem_dia_diem:".length()).trim();
        } else if (trim.startsWith("xem_gia:")) {
            action = "gia";
            slug = trim.substring("xem_gia:".length()).trim();
        }
        if (slug == null || slug.isBlank()) return null;
        return tourRepository.findBySlug(slug).map(tour -> {
            String reply = null;
            if ("lich_trinh".equals(action)) {
                Hibernate.initialize(tour.getItineraries());
                List<TourItinerary> list = tour.getItineraries() != null ? tour.getItineraries().stream()
                        .sorted(Comparator.comparing(TourItinerary::getDayNumber, Comparator.nullsFirst(Integer::compareTo)))
                        .toList() : List.<TourItinerary>of();
                if (list.isEmpty()) reply = "Tour **" + tour.getTitle() + "** hiện chưa có lịch trình chi tiết. Bạn xem trang tour để cập nhật nhé.";
                else {
                    StringBuilder sb = new StringBuilder("Lịch trình **").append(tour.getTitle()).append("**:\n\n");
                    for (TourItinerary day : list) {
                        sb.append("**Ngày ").append(day.getDayNumber()).append(":** ").append(day.getTitle() != null ? day.getTitle() : "").append("\n");
                        if (day.getDescription() != null && !day.getDescription().isBlank())
                            sb.append(day.getDescription().trim()).append("\n");
                        sb.append("\n");
                    }
                    sb.append("Xem chi tiết và đặt tour tại trang tour nhé.");
                    reply = sb.toString();
                }
            } else if ("dia_diem".equals(action)) {
                Hibernate.initialize(tour.getLocations());
                List<TourLocation> locs = tour.getLocations() != null ? tour.getLocations().stream()
                        .sorted(Comparator.comparing(TourLocation::getVisitOrder, Comparator.nullsFirst(Integer::compareTo)))
                        .toList() : List.<TourLocation>of();
                if (locs.isEmpty()) reply = "Tour **" + tour.getTitle() + "** ghé các điểm theo lịch trình từng ngày. Bạn xem mục Lịch trình hoặc trang chi tiết tour nhé.";
                else {
                    StringBuilder sb = new StringBuilder("Các địa điểm **").append(tour.getTitle()).append("** ghé thăm:\n\n");
                    for (TourLocation loc : locs)
                        sb.append("• ").append(loc.getLocationName() != null ? loc.getLocationName() : "").append("\n");
                    sb.append("\nXem bản đồ và chi tiết trên trang tour.");
                    reply = sb.toString();
                }
            } else if ("gia".equals(action)) {
                long price = tour.getBasePrice() != null ? tour.getBasePrice().longValue() : 0L;
                String priceStr = price > 0 ? String.format("%,d", price) + "₫" : "liên hệ";
                reply = "**" + tour.getTitle() + "**: giá từ " + priceStr
                        + (tour.getDurationDays() != null ? " (" + tour.getDurationDays() + " ngày)." : ".")
                        + " Giá có thể thay đổi theo ngày khởi hành và option. Bạn xem trang tour hoặc để lại SĐT để nhận báo giá chính xác nhé.";
            }
            if (reply == null) return (ChatbotResponse) null;
            List<ChatbotResponse.QuickReply> qr = List.of(
                    ChatbotResponse.QuickReply.builder().label("Xem chi tiết tour").payload("Xem chi tiết tour").build(),
                    ChatbotResponse.QuickReply.builder().label("Tour khác").payload("Xem thêm tour").build()
            );
            return ChatbotResponse.builder().reply(reply).quickReplies(qr).build();
        }).orElse(null);
    }

    /** Câu hỏi dạng "tháng X đi Y đẹp không" / "nên đi đâu" – có địa điểm thì tra DB tour và gợi ý hoặc báo không có. */
    private static boolean isBestTimeOrGeneralPlaceQuestion(String content) {
        if (content == null || content.isBlank()) return false;
        String lower = content.toLowerCase();
        boolean hasTime = lower.contains("tháng ") || lower.contains("mùa nào") || lower.contains("thời điểm") || lower.contains("nên đi");
        boolean hasPlace = lower.contains("đà lạt") || lower.contains("da lat") || lower.contains("đà nẵng") || lower.contains("da nang")
                || lower.contains("hà giang") || lower.contains("ha giang") || lower.contains("phú quốc") || lower.contains("phu quoc")
                || lower.contains("nha trang") || lower.contains("hội an") || lower.contains("hoi an") || lower.contains("sapa") || lower.contains("sa pa")
                || lower.contains("phan thiết") || lower.contains("mui ne") || lower.contains("hạ long") || lower.contains("ha long")
                || lower.contains("ninh bình") || lower.contains("ninh binh") || lower.contains("vũng tàu") || lower.contains("vung tau")
                || lower.contains("huế") || lower.contains("hue") || lower.contains("đi đâu") || lower.contains("ở đâu");
        return hasTime && (hasPlace || lower.contains("đẹp") || lower.contains("ít mưa") || lower.contains("mưa"));
    }

    /** Trả lời best_time/general: reply đúng chủ đề; nếu có tour ở địa điểm thì kèm tour + chú thích; không thì báo và gợi ý tour khác. */
    private ChatbotResponse buildBestTimeOrGeneralPlaceResponse(String policyReply, String content, ChatbotRequest request) {
        String destination = extractSearchKeyword(content);
        if (destination == null || destination.isBlank() || "tour".equals(destination) || "biển".equals(destination))
            return null;
        List<ChatbotResponse.TourCard> toursAtDest = searchToursWithDuration(destination, null, null, null, 6);
        List<ChatbotResponse.QuickReply> qr = new ArrayList<>();
        qr.add(ChatbotResponse.QuickReply.builder().label("Xem thêm tour").payload("Xem thêm tour").build());
        qr.add(ChatbotResponse.QuickReply.builder().label("Chính sách hủy/đổi").payload("Chính sách hủy tour").build());
        String reply = policyReply;
        List<ChatbotResponse.TourCard> toursToShow = new ArrayList<>();
        if (toursAtDest.isEmpty()) {
            reply = policyReply + "\n\nHiện tại không có tour nào ở " + destination + ". Bạn có thể tham khảo một số tour sau:";
            toursToShow = searchToursWithDuration(null, null, null, null, 4);
        } else {
            List<Tour> tourEntities = searchTourEntities(destination, null, null, null, 6);
            toursToShow = tourEntities.stream().map(t -> toTourCard(t, true)).toList();
            Map<String, Object> state = new HashMap<>();
            state.put("lastSuggestedTourSlug", toursToShow.isEmpty() ? null : toursToShow.get(0).getSlug());
            state.put("intent", "best_time");
            return ChatbotResponse.builder().reply(reply).tours(toursToShow).quickReplies(qr).state(state).build();
        }
        return ChatbotResponse.builder()
                .reply(reply)
                .tours(toursToShow)
                .quickReplies(qr)
                .build();
    }

    /** Chuẩn hóa câu để match training phrase tốt hơn: bỏ dấu thừa, slang, typo phổ biến. */
    private static String normalizeForMatching(String s) {
        if (s == null || s.isBlank()) return "";
        String t = s.toLowerCase().trim().replaceAll("\\s+", " ");
        t = t.replace("hoan tien", "hoàn tiền").replace("hoan lai", "hoàn lại").replace("tra lai tien", "trả lại tiền");
        t = t.replace("huy tour", "hủy tour").replace("chinh sach huy", "chính sách hủy").replace("huy truoc", "hủy trước");
        t = t.replace("doi ngay", "đổi ngày").replace("doi tour", "đổi tour").replace("doi ten", "đổi tên").replace("doi phong", "đổi phòng");
        t = t.replace("tra gop", "trả góp").replace("thanh toan", "thanh toán").replace("dat coc", "đặt cọc");
        t = t.replace("tre em", "trẻ em").replace("ve may bay", "vé máy bay").replace("bao gom", "bao gồm");
        t = t.replace("huong dan vien", "hướng dẫn viên").replace("ho chieu", "hộ chiếu");
        t = t.replace("bao hiem", "bảo hiểm").replace("tỉ gia", "tỉ giá").replace("doi tien", "đổi tiền");
        t = t.replace("bao do bo", "bão đổ bộ").replace("do bo", "đổ bộ");
        t = t.replace(" ko ", " không ").replace(" ko\"", " không").replace(" dc ", " được ").replace(" dc\"", " được");
        t = t.replace("khong", "không").replace("duoc", "được").replace("bao nhieu", "bao nhiêu");
        t = t.replace("khoi hanh", "khởi hành").replace("ho chieu", "hộ chiếu").replace("thoi tiet", "thời tiết");
        return t;
    }

    private String getPolicyReply(String userMessage) {
        if (userMessage == null) return null;
        String normalized = normalizeForMatching(userMessage);
        if (!looksLikePolicyQuestion(normalized)) return null;
        String lower = normalized;
        List<PolicyFaq> all = policyFaqRepository.findAllByOrderBySortOrderAsc();
        if (all.isEmpty()) return null;
        // Training phrases: match câu mẫu (ưu tiên phrase dài nhất); dùng cả lower và normalized
        List<ChatbotTrainingPhrase> phrases = trainingPhraseRepository.findAllByOrderByPhraseAsc();
        String matchTopic = phrases.stream()
                .filter(p -> {
                    if (p.getPhrase() == null || p.getPhrase().isBlank()) return false;
                    String phraseNorm = normalizeForMatching(p.getPhrase());
                    return normalized.contains(phraseNorm) || lower.contains(p.getPhrase().toLowerCase().trim());
                })
                .max(Comparator.comparingInt(p -> p.getPhrase().length()))
                .map(ChatbotTrainingPhrase::getTopicKey)
                .orElse(null);
        if (matchTopic != null) {
            String reply = findByTopic(all, matchTopic);
            if (reply != null) return reply;
        }
        // In-tour / urgent first (đổi phòng, quên đồ, khẩn cấp, liên hệ người)
        if (lower.contains("đổi phòng") || lower.contains("điều hòa") || lower.contains("hỏng") && (lower.contains("phòng") || lower.contains("máy lạnh"))
                || lower.contains("hướng dẫn viên") || lower.contains("hdv") && (lower.contains("không tìm") || lower.contains("điểm hẹn"))
                || lower.contains("quên ") || lower.contains("để quên") || lower.contains("thất lạc") || lower.contains("hộ chiếu") && lower.contains("xe"))
            return findByTopic(all, "in_tour_support");
        if (lower.contains("khẩn cấp") || lower.contains("liên hệ nhân viên") || lower.contains("gặp người") || lower.contains("chuyển người") || lower.contains("tư vấn viên"))
            return findByTopic(all, "human_handoff");
        // Crisis: weather, health, lost
        if (lower.contains("bão") || lower.contains("mưa lớn") || (lower.contains("tour") && lower.contains("khởi hành") && (lower.contains("không") || lower.contains("có "))) || lower.contains("đền bù")) {
            return findByTopic(all, "crisis_weather");
        }
        if (lower.contains("say sóng") || (lower.contains("đường bộ") && lower.contains("đổi")) || lower.contains("ngộ độc") || lower.contains("cấp cứu")) {
            return findByTopic(all, "crisis_health");
        }
        if (lower.contains("mất ví") || lower.contains("mất hộ chiếu") || lower.contains("trình báo công an") || lower.contains("trình báo ở đâu"))
            return findByTopic(all, "crisis_lost");
        // Visa / passport / insurance
        if (lower.contains("visa") || lower.contains("hộ chiếu") || lower.contains("passport") || lower.contains("còn hạn"))
            return findByTopic(all, "visa");
        if (lower.contains("bảo hiểm") || lower.contains("bồi thường") || (lower.contains("tai nạn") && lower.contains("nước ngoài")))
            return findByTopic(all, "insurance");
        // Policy: cancel, change, refund, payment, children
        if (lower.contains("hủy") || lower.contains("cancel") || lower.contains("ốm") && (lower.contains("đi được") || lower.contains("không đi")))
            return findByTopic(all, "cancellation");
        if (lower.contains("đổi ngày") || lower.contains("đổi tour") || lower.contains("đổi lịch") || lower.contains("dời ngày") || lower.contains("đổi tên"))
            return findByTopic(all, "change_date");
        if (lower.contains("hoàn tiền") || lower.contains("hoàn lại") || lower.contains("refund") || lower.contains("trả lại tiền"))
            return findByTopic(all, "refund");
        if (lower.contains("trả góp") || lower.contains("thẻ tín dụng") || lower.contains("mất phí") || lower.contains("chuyển đổi"))
            return findByTopic(all, "payment");
        if (lower.contains("thanh toán") || lower.contains("payment") || lower.contains("đặt cọc") || lower.contains("chuyển khoản"))
            return findByTopic(all, "payment");
        if (lower.contains("trẻ em") || lower.contains("trẻ con") || lower.contains("trẻ nhỏ") || lower.contains("con nhỏ") || lower.contains("giường riêng") || lower.contains("bé ") || lower.contains(" bé "))
            return findByTopic(all, "children");
        // Pre-booking info: itinerary, compare, best_time, age_fitness, what_included
        if (lower.contains("lịch trình") || lower.contains("ngày 2") || lower.contains("ngày 3") || (lower.contains("đi đâu") && lower.contains("ngày")) || lower.contains("thời gian tự do"))
            return findByTopic(all, "itinerary");
        if (lower.contains("so sánh") || lower.contains("standard") && lower.contains("pro") || lower.contains("đắt hơn") || lower.contains("khác nhau"))
            return findByTopic(all, "compare");
        if (lower.contains("tháng ") && (lower.contains("đi ") || lower.contains("đẹp") || lower.contains("mưa")) || lower.contains("mùa nào") || lower.contains("ít mưa"))
            return findByTopic(all, "best_time");
        if (lower.contains("người già") || lower.contains("vất vả") || lower.contains("ăn chay") || lower.contains("thực đơn"))
            return findByTopic(all, "age_fitness");
        if (lower.contains("giá đã") || lower.contains("bao gồm") || lower.contains("vé máy bay") || lower.contains("tip") && (lower.contains("bắt buộc") || lower.contains("tự nguyện")) || lower.contains("chi phí ẩn"))
            return findByTopic(all, "what_included");
        if (lower.contains("xe đưa đón") || lower.contains("vé show") || lower.contains("ký ức hội an") || lower.contains("sim 4g") || lower.contains("sim ") && lower.contains("thái") || lower.contains("dịch vụ thêm"))
            return findByTopic(all, "addons");
        if (lower.contains("mang theo") || lower.contains("hành trang") || lower.contains("áo phao") || lower.contains("giữ nhiệt") || (lower.contains("fansipan") && lower.contains("lạnh")))
            return findByTopic(all, "packing_tips");
        if ((lower.contains("singapore") && (lower.contains("kẹo") || lower.contains("cao su"))) || (lower.contains("thái lan") && (lower.contains("váy") || lower.contains("đền") || lower.contains("chùa"))))
            return findByTopic(all, "culture_rules");
        if (lower.contains("đổi tiền") || lower.contains("tỉ giá") || lower.contains("won") || lower.contains("đổi ngoại tệ"))
            return findByTopic(all, "money_exchange");
        if (lower.contains("ổ cắm") || lower.contains("2 chấu") || lower.contains("3 chấu") || lower.contains("cục chuyển"))
            return findByTopic(all, "practical_tips");
        if (lower.contains("quán ẩn") || lower.contains("ốc ngon") || lower.contains("dân địa phương") || lower.contains("góc check-in") || (lower.contains("bình minh") && (lower.contains("đà lạt") || lower.contains("chụp"))) || lower.contains("trả giá") || (lower.contains("taxi") && (lower.contains("chặt") || lower.contains("chém"))))
            return findByTopic(all, "local_secrets");
        if ((lower.contains("usd") || lower.contains("đổi ra")) && (lower.contains("baht") || lower.contains("bao nhiêu")) || lower.contains("tỉ giá hôm nay"))
            return findByTopic(all, "currency_convert");
        if (lower.contains("dự báo thời tiết") || (lower.contains("thời tiết") && (lower.contains("3 ngày") || lower.contains("theo giờ"))))
            return findByTopic(all, "weather_forecast");
        if (lower.contains("dịch") && (lower.contains("tiếng thái") || lower.contains("phiên âm") || lower.contains("nói bằng")))
            return findByTopic(all, "translate_phrase");
        if (lower.contains("chuyến đi vừa rồi") || (lower.contains("review") && lower.contains("chuyến")) || (lower.contains("chia sẻ ảnh") && lower.contains("voucher"))) {
            return findByTopic(all, "post_trip_review");
        }
        if (lower.contains("không hài lòng") || (lower.contains("thái độ") && lower.contains("hdv")) || lower.contains("phản ánh") || lower.contains("khiếu nại")) {
            return findByTopic(all, "post_trip_complaint");
        }
        if (lower.contains("gợi ý lần sau") || (lower.contains("lễ hội") && (lower.contains("nha trang") || lower.contains("tháng sau")))) {
            return findByTopic(all, "post_trip_next");
        }
        if ((lower.contains("bên ") && (lower.contains("rẻ hơn") || lower.contains("đặc biệt"))) || (lower.contains("tại sao") && (lower.contains("đắt") || lower.contains("xa"))) || (lower.contains("khách sạn") && lower.contains("xa"))) {
            return findByTopic(all, "competitive");
        }
        if (lower.contains("hoạt động bao lâu") || lower.contains("giấy phép kinh doanh"))
            return findByTopic(all, "trust");
        if (lower.contains("chính sách")) return all.stream().filter(p -> "cancellation".equals(p.getTopicKey())).findFirst().map(PolicyFaq::getContent).orElse(all.get(0).getContent());
        return null;
    }

    private static String findByTopic(List<PolicyFaq> all, String topicKey) {
        return all.stream().filter(p -> topicKey.equals(p.getTopicKey())).findFirst().map(PolicyFaq::getContent).orElse(all.get(0).getContent());
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
            log.warn("Failed to save search log", e);
        }
    }

    private String getString(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v != null ? v.toString().trim() : null;
    }

    private ChatbotResponse.TourCard toTourCard(Tour t) {
        return toTourCard(t, false);
    }

    /** Tour card có thêm nút Lịch trình, Địa điểm, Giá cả khi withActions=true (sau khi AI gợi ý địa điểm có tour). */
    private ChatbotResponse.TourCard toTourCard(Tour t, boolean withActions) {
        String imageUrl = null;
        if (t.getImages() != null && !t.getImages().isEmpty()) {
            TourImage first = t.getImages().get(0);
            if (first != null) imageUrl = first.getImageUrl();
        }
        List<ChatbotResponse.QuickReply> actions = null;
        if (withActions && t.getSlug() != null && !t.getSlug().isBlank()) {
            String slug = t.getSlug();
            actions = List.of(
                    ChatbotResponse.QuickReply.builder().label("Lịch trình").payload("xem_lich_trinh:" + slug).build(),
                    ChatbotResponse.QuickReply.builder().label("Địa điểm").payload("xem_dia_diem:" + slug).build(),
                    ChatbotResponse.QuickReply.builder().label("Giá cả").payload("xem_gia:" + slug).build()
            );
        }
        return ChatbotResponse.TourCard.builder()
                .id(t.getId().toString())
                .title(t.getTitle())
                .slug(t.getSlug())
                .price(t.getBasePrice() != null ? t.getBasePrice().longValue() : 0L)
                .durationDays(t.getDurationDays())
                .imageUrl(imageUrl)
                .actions(actions)
                .build();
    }

    private static final String OFF_TOPIC_REPLY = "Mình là trợ lý của FlourishTravel, chỉ tư vấn tour du lịch và chính sách đặt/hủy tour thôi ạ. Bạn muốn tìm tour hay hỏi chính sách? Thử chọn gợi ý bên dưới nhé.";

    private static boolean looksLikeTravelOrPolicy(String content) {
        if (content == null || content.isBlank()) return false;
        String lower = content.toLowerCase();
        return lower.contains("tour") || lower.contains("du lịch") || lower.contains("du lich") || lower.contains("đi ") || lower.contains("đặt tour")
                || lower.contains("chính sách") || lower.contains("hủy") || lower.contains("đổi ngày") || lower.contains("hoàn tiền") || lower.contains("thanh toán")
                || lower.contains("biển") || lower.contains("đà lạt") || lower.contains("đà nẵng") || lower.contains("phan thiết") || lower.contains("nha trang")
                || lower.contains("phú quốc") || lower.contains("hạ long") || lower.contains("hội an") || lower.contains("vũng tàu") || lower.contains("sapa")
                || lower.contains("ninh bình") || lower.contains("huế") || lower.contains("côn đảo") || lower.contains("quy nhơn") || lower.contains("cần thơ")
                || lower.contains("trẻ em") || lower.contains("ngày") && (lower.contains("đi") || lower.contains("tour"));
    }

    private ChatbotResponse fallbackResponse(String content) {
        String policyReply = getPolicyReply(content);
        if (policyReply != null) {
            return buildPolicyOnlyResponse(policyReply, looksLikeHumanHandoff(content));
        }
        if (!looksLikeTravelOrPolicy(content)) {
            return ChatbotResponse.builder()
                    .reply(OFF_TOPIC_REPLY)
                    .tours(List.of())
                    .quickReplies(List.of(
                            ChatbotResponse.QuickReply.builder().label("Tour biển 3 ngày").payload("Tour biển 3 ngày").build(),
                            ChatbotResponse.QuickReply.builder().label("Chính sách hủy tour").payload("Chính sách hủy tour").build(),
                            ChatbotResponse.QuickReply.builder().label("Tour Đà Lạt").payload("Tour Đà Lạt").build()
                    ))
                    .build();
        }
        String keyword = extractSearchKeyword(content);
        String destinationPattern = (keyword != null && !keyword.isBlank()) ? "%" + keyword.trim() + "%" : null;
        var page = tourRepository.searchForSuggestion(
                destinationPattern,
                null, null, null,
                PageRequest.of(0, 8));
        List<ChatbotResponse.TourCard> cards = page.getContent().stream().map(this::toTourCard).collect(Collectors.toList());
        String reply = cards.isEmpty()
                ? "Hiện chưa có tour nào khớp với yêu cầu của bạn. Bạn thử gợi ý bên dưới hoặc để lại thông tin để mình tư vấn nhé."
                : "Dựa trên yêu cầu của bạn, đây là một số tour phù hợp. Bạn có thể xem chi tiết hoặc thử tìm theo địa điểm/ số ngày khác nhé.";
        Map<String, Object> state = new HashMap<>();
        if (!cards.isEmpty() && cards.get(0).getSlug() != null) {
            state.put("lastSuggestedTourSlug", cards.get(0).getSlug());
            state.put("context", "awaiting_tour_selection");
        }
        return ChatbotResponse.builder()
                .reply(reply)
                .tours(cards)
                .quickReplies(List.of(
                        ChatbotResponse.QuickReply.builder().label("Tour biển 3 ngày").payload("Tour biển 3 ngày").build(),
                        ChatbotResponse.QuickReply.builder().label("Tour Đà Lạt").payload("Tour Đà Lạt").build(),
                        ChatbotResponse.QuickReply.builder().label("Tour Phan Thiết").payload("Tour Phan Thiết").build()
                ))
                .state(state.isEmpty() ? null : state)
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
        if (lower.contains("miền trung") || lower.contains("mien trung")) return "Đà Nẵng";
        if (lower.contains("miền bắc") || lower.contains("mien bac")) return "Hạ Long";
        if (lower.contains("miền nam") || lower.contains("mien nam")) return "Phú Quốc";
        if (lower.contains("hà giang") || lower.contains("ha giang")) return "Hà Giang";
        if (lower.contains("biển") || lower.contains("bãi") || lower.contains("tour biển")) return "biển";
        if (lower.contains("tour") && content.length() > 3) return "tour";
        return content.length() > 2 ? content : null;
    }

    /** Match user message với training phrases. Có state từ câu trước thì ưu tiên intent theo context (câu tiếp nối). */
    private ChatbotIntentWithPhrases matchIntentFromTrainingPhrases(String content, ChatbotRequest request) {
        if (content == null || content.isBlank()) return null;
        String normalized = normalizeForMatching(content);
        String lower = content.toLowerCase().trim();
        List<ChatbotIntentWithPhrases> all = chatbotConfigService.getIntentsWithPhrases();

        // Câu tiếp nối: state có context → ưu tiên intent theo nội dung câu hỏi
        Map<String, Object> state = request.getState();
        if (state != null && state.get("context") != null && content.length() <= 80) {
            String ctx = String.valueOf(state.get("context"));
            boolean followUpBooking = lower.contains("thêm") && (lower.contains("đêm") || lower.contains("ngày"))
                    || lower.contains("phí") || lower.contains("giữ chỗ") || lower.contains("đặt ") || lower.contains("bao nhiêu")
                    || lower.contains("tính phí") || lower.contains("giá") && lower.contains("thêm");
            if (("awaiting_tour_selection".equals(ctx) || "booking_in_progress".equals(ctx)) && followUpBooking) {
                for (ChatbotIntentWithPhrases iwp : all) {
                    if ("multi_intent_booking_upsell".equals(iwp.intent().getIntentName()))
                        return iwp;
                }
            }
            // Đang chọn tour, user hỏi "lịch trình như thế nào" → ưu tiên intent lịch trình (tour vừa gợi ý)
            boolean followUpItinerary = lower.contains("lịch trình") || lower.contains("lich trinh")
                    || lower.contains("ngày 2 đi đâu") || lower.contains("ngày 1 đi");
            if ("awaiting_tour_selection".equals(ctx) && followUpItinerary) {
                for (ChatbotIntentWithPhrases iwp : all) {
                    if ("itinerary_detail".equals(iwp.intent().getIntentName()))
                        return iwp;
                }
            }
        }

        ChatbotIntentWithPhrases best = null;
        int bestLen = 0;
        for (ChatbotIntentWithPhrases iwp : all) {
            for (String phrase : iwp.phrases()) {
                if (phrase == null || phrase.isBlank()) continue;
                String pNorm = normalizeForMatching(phrase);
                if (pNorm.length() < 2) continue;
                boolean match = normalized.contains(pNorm) || lower.contains(phrase.toLowerCase().trim());
                if (match && phrase.length() > bestLen) {
                    bestLen = phrase.length();
                    best = iwp;
                }
            }
        }
        return best;
    }

    /** Tạo response từ intent đã match: điền template, gọi system_action (tour search...), trả state. */
    private ChatbotResponse buildResponseFromIntent(ChatbotIntentWithPhrases iwp, String content, ChatbotRequest request) {
        ChatbotIntent intent = iwp.intent();
        String template = intent.getResponseTemplate();
        if (template == null || template.isBlank()) template = "Mình đã ghi nhận yêu cầu của bạn. Bạn cần thêm thông tin gì không?";

        Map<String, Object> slots = extractSlotsForIntent(content, intent, request);
        // Template có {issue} nhưng intent không khai báo entity → lấy từ nội dung user
        if (template != null && template.contains("{issue}") && (slots.get("issue") == null || "...".equals(String.valueOf(slots.get("issue"))))) {
            String issueDesc = content.length() > 100 ? content.substring(0, 97).trim() + "…" : content;
            slots.put("issue", issueDesc);
        }

        List<ChatbotResponse.TourCard> tours = List.of();
        String systemActionJson = intent.getSystemAction();
        if (systemActionJson != null && !systemActionJson.isBlank()) {
            try {
                Map<String, Object> action = objectMapper.readValue(systemActionJson, new TypeReference<>() {});
                String type = action != null ? (String) action.get("type") : null;
                String endpoint = action != null ? (String) action.get("api_endpoint") : null;
                String dest = slots.get("destination") != null ? String.valueOf(slots.get("destination")) : null;
                if (dest == null || dest.isBlank()) dest = extractSearchKeyword(content);

                if ("database_query".equals(type) && endpoint != null && endpoint.contains("tour")) {
                    BigDecimal min = slots.get("budget_min") instanceof Number n ? BigDecimal.valueOf(n.doubleValue() * 1_000_000) : null;
                    BigDecimal max = slots.get("budget_max") instanceof Number n ? BigDecimal.valueOf(n.doubleValue() * 1_000_000) : null;
                    Integer days = slots.get("duration_days") instanceof Number n ? n.intValue() : null;
                    if (days == null && slots.get("duration") instanceof Number n) days = n.intValue();
                    tours = searchToursWithDuration(dest, min, max, days, 6);
                    saveSearchLog(request, content, dest, min, max, days, tours.size());
                } else if ("check_realtime_inventory".equals(type) && endpoint != null && (endpoint.contains("availability") || endpoint.contains("bookings"))) {
                    var avOpt = tourService.checkAvailability(dest, null);
                    if (avOpt.isPresent()) {
                        var av = avOpt.get();
                        slots.put("remaining_slots", av.getRemainingSlots() != null ? av.getRemainingSlots() : "ít");
                        slots.put("next_start_date", av.getNextStartDate() != null ? av.getNextStartDate().toString() : null);
                        slots.put("date", av.getNextStartDate() != null ? av.getNextStartDate().toString() : null);
                    } else {
                        slots.put("remaining_slots", "ít");
                    }
                } else if ("external_api_call".equals(type) && endpoint != null) {
                    if (endpoint.toLowerCase().contains("place") || endpoint.toLowerCase().contains("maps") || endpoint.toLowerCase().contains("nearby")) {
                        var place = chatbotDataService.getNearbyPlace(dest, (String) slots.get("poi_type"));
                        if (place != null) {
                            slots.put("distance", place.getDistance());
                            slots.put("poi_type", place.getType() != null ? place.getType() : slots.get("poi_type"));
                            slots.put("rating", place.getRating() != null ? place.getRating() + " sao" : null);
                            slots.put("place_name", place.getName());
                        }
                    } else if (endpoint.toLowerCase().contains("weather")) {
                        var weather = chatbotDataService.getWeatherForecast(dest);
                        if (weather != null) slots.put("weather_summary", weather.getSummary());
                    }
                }
            } catch (Exception e) {
                log.debug("Parse system_action failed: {}", e.getMessage());
            }
        }

        String reply;
        if ("itinerary_detail".equals(intent.getIntentName())) {
            String itineraryReply = buildItineraryReply(request, slots, content);
            if (itineraryReply != null && !itineraryReply.isBlank()) {
                reply = itineraryReply;
            } else {
                fillDestinationFromLastTour(request, slots);
                reply = fillTemplate(template, slots);
            }
        } else {
            reply = fillTemplate(template, slots);
        }

        Map<String, Object> state = new HashMap<>();
        state.put("intent", intent.getIntentName());
        state.put("slots", slots);
        if (intent.getContextOutput() != null && !intent.getContextOutput().isBlank()) {
            state.put("context", intent.getContextOutput());
        }
        if (!tours.isEmpty() && tours.get(0).getSlug() != null) {
            state.put("lastSuggestedTourSlug", tours.get(0).getSlug());
        }

        List<ChatbotResponse.QuickReply> qr = buildQuickRepliesForIntent(intent);

        return ChatbotResponse.builder()
                .reply(reply)
                .tours(tours)
                .quickReplies(qr)
                .state(state)
                .build();
    }

    /** Rút số ngày từ câu (vd. "ngày 2 đi đâu?" -> 2). */
    private static Integer extractDayNumberFromContent(String content) {
        if (content == null || content.isBlank()) return null;
        Matcher m = Pattern.compile("ngày\\s*(\\d+)", Pattern.CASE_INSENSITIVE).matcher(content);
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }

    /** Điền slot destination = tên tour từ lastSuggestedTourSlug để template không ra "tour tour". */
    private void fillDestinationFromLastTour(ChatbotRequest request, Map<String, Object> slots) {
        Map<String, Object> prevState = request.getState();
        if (prevState == null) return;
        Object slugObj = prevState.get("lastSuggestedTourSlug");
        String slug = slugObj != null ? String.valueOf(slugObj).trim() : null;
        if (slug == null || slug.isBlank()) return;
        tourRepository.findBySlug(slug).ifPresent(t -> slots.put("destination", t.getTitle()));
    }

    /** Lấy lịch trình từng ngày của tour (theo lastSuggestedTourSlug). Nếu content có "ngày X" thì chỉ trả ngày đó. */
    private String buildItineraryReply(ChatbotRequest request, Map<String, Object> slots, String content) {
        Map<String, Object> prevState = request.getState();
        if (prevState == null) return null;
        Object slugObj = prevState.get("lastSuggestedTourSlug");
        String slug = slugObj != null ? String.valueOf(slugObj).trim() : null;
        if (slug == null || slug.isBlank()) return null;
        Integer askDay = extractDayNumberFromContent(content);
        return tourRepository.findBySlug(slug)
                .map(tour -> {
                    Hibernate.initialize(tour.getItineraries());
                    List<TourItinerary> list = tour.getItineraries() != null ? tour.getItineraries().stream()
                            .sorted(Comparator.comparing(TourItinerary::getDayNumber, Comparator.nullsFirst(Integer::compareTo)))
                            .toList() : List.<TourItinerary>of();
                    if (list.isEmpty()) return null;
                    List<TourItinerary> toShow = askDay != null
                            ? list.stream().filter(d -> askDay.equals(d.getDayNumber())).toList()
                            : list;
                    if (toShow.isEmpty() && askDay != null) {
                        return "Tour **" + tour.getTitle() + "** có " + list.size() + " ngày. Bạn hỏi ngày 1 đến " + list.size() + " nhé.";
                    }
                    if (toShow.isEmpty()) return null;
                    StringBuilder sb = new StringBuilder();
                    if (askDay != null && toShow.size() == 1) {
                        TourItinerary day = toShow.get(0);
                        sb.append("**Ngày ").append(day.getDayNumber()).append("** của **").append(tour.getTitle()).append("**:\n\n");
                        sb.append(day.getTitle() != null ? day.getTitle() : "").append("\n");
                        if (day.getDescription() != null && !day.getDescription().isBlank()) {
                            sb.append(day.getDescription().trim()).append("\n");
                        }
                    } else {
                        sb.append("Lịch trình **").append(tour.getTitle()).append("**:\n\n");
                        for (TourItinerary day : toShow) {
                            sb.append("**Ngày ").append(day.getDayNumber()).append(":** ").append(day.getTitle() != null ? day.getTitle() : "").append("\n");
                            if (day.getDescription() != null && !day.getDescription().isBlank()) {
                                sb.append(day.getDescription().trim()).append("\n");
                            }
                            sb.append("\n");
                        }
                    }
                    sb.append("Bạn có thể xem chi tiết và đặt tour trên trang tour nhé.");
                    return sb.toString();
                })
                .orElse(null);
    }

    /** Quick replies theo từng intent để nút gợi ý sát với câu hỏi (sau khi trả lời được). */
    private List<ChatbotResponse.QuickReply> buildQuickRepliesForIntent(ChatbotIntent intent) {
        List<ChatbotResponse.QuickReply> qr = new ArrayList<>();
        String name = intent.getIntentName();
        if ("in_tour_crisis_handling".equals(name)) {
            qr.add(ChatbotResponse.QuickReply.builder().label("Liên hệ hotline khẩn cấp").payload("Liên hệ nhân viên").build());
            qr.add(ChatbotResponse.QuickReply.builder().label("Chính sách bồi thường").payload("Chính sách hủy tour").build());
        } else if ("itinerary_detail".equals(name)) {
            qr.add(ChatbotResponse.QuickReply.builder().label("Xem chi tiết tour").payload("Xem chi tiết tour").build());
            qr.add(ChatbotResponse.QuickReply.builder().label("Ngày 2 đi đâu?").payload("Tour này ngày 2 đi đâu?").build());
            qr.add(ChatbotResponse.QuickReply.builder().label("Tour khác").payload("Xem thêm tour").build());
        } else if ("best_time_to_visit".equals(name)) {
            qr.add(ChatbotResponse.QuickReply.builder().label("Xem tour theo tháng").payload("Xem thêm tour").build());
            qr.add(ChatbotResponse.QuickReply.builder().label("Chính sách hủy/đổi").payload("Chính sách hủy tour").build());
        } else if ("policy_cancellation".equals(name) || "policy_refund".equals(name) || "policy_change_date".equals(name)) {
            qr.add(ChatbotResponse.QuickReply.builder().label("Chính sách đổi ngày").payload("Chính sách đổi ngày").build());
            qr.add(ChatbotResponse.QuickReply.builder().label("Hoàn tiền").payload("Hoàn tiền").build());
            qr.add(ChatbotResponse.QuickReply.builder().label("Tour biển 3 ngày").payload("Tour biển 3 ngày").build());
        } else {
            qr.add(ChatbotResponse.QuickReply.builder().label("Xem thêm tour").payload("Xem thêm tour").build());
            qr.add(ChatbotResponse.QuickReply.builder().label("Chính sách hủy/đổi").payload("Chính sách hủy tour").build());
        }
        return qr;
    }

    private Map<String, Object> extractSlotsForIntent(String content, ChatbotIntent intent, ChatbotRequest request) {
        Map<String, Object> slots = new HashMap<>();
        if (request.getState() != null && request.getState().get("slots") instanceof Map<?, ?> prev) {
            prev.forEach((k, v) -> {
                if (v != null) slots.put(String.valueOf(k), v);
            });
        }
        List<String> entityNames = parseEntitiesList(intent.getEntitiesToExtract());
        if (entityNames.isEmpty()) return slots;
        String prompt = "User nói: \"" + content.replace("\"", "'") + "\". Trích xuất các thông tin sau (chỉ trả JSON, không markdown): ";
        prompt += String.join(", ", entityNames);
        prompt += ". Trả lời ĐÚNG 1 JSON: {\"slots\":{" + entityNames.stream().map(e -> "\"" + e + "\": giá trị hoặc null").collect(Collectors.joining(", ")) + "}}";
        Map<String, Object> llmJson = llmService.generateJson(prompt);
        if (llmJson != null && llmJson.get("slots") instanceof Map<?, ?> m) {
            m.forEach((k, v) -> {
                if (v != null && !"null".equals(String.valueOf(v))) slots.put(String.valueOf(k), v);
            });
        }
        extractSlotsFromKeywords(content, slots);
        fillBestPeriodForIntent(intent, slots);
        return slots;
    }

    /** Điền best_period cho intent best_time_to_visit theo địa điểm (tránh trả lời nhầm "Đà Nẵng" khi user hỏi Hà Giang/Phú Quốc). */
    private void fillBestPeriodForIntent(ChatbotIntent intent, Map<String, Object> slots) {
        if (!"best_time_to_visit".equals(intent.getIntentName())) return;
        Object destObj = slots.get("destination");
        String dest = destObj != null ? String.valueOf(destObj).trim() : null;
        if (dest == null || dest.isBlank()) return;
        String best = getBestPeriodByDestination(dest);
        if (best != null) slots.put("best_period", best);
    }

    private static final Map<String, String> BEST_PERIOD_MAP = Map.ofEntries(
            Map.entry("Hà Giang", "tháng 9–10 (mùa lúa chín) hoặc tháng 12–1 (mùa tam giác mạch)"),
            Map.entry("Ha Giang", "tháng 9–10 (mùa lúa chín) hoặc tháng 12–1 (mùa tam giác mạch)"),
            Map.entry("Phú Quốc", "tháng 11 đến tháng 4 (ít mưa, nắng đẹp)"),
            Map.entry("Phu Quoc", "tháng 11 đến tháng 4 (ít mưa, nắng đẹp)"),
            Map.entry("Đà Nẵng", "tháng 2–8 (ít mưa, biển đẹp)"),
            Map.entry("Da Nang", "tháng 2–8 (ít mưa, biển đẹp)"),
            Map.entry("Đà Lạt", "tháng 11–4 (khô, hoa dã quỳ; tháng 3–4 hoa phượng tím)"),
            Map.entry("Da Lat", "tháng 11–4 (khô, hoa dã quỳ; tháng 3–4 hoa phượng tím)"),
            Map.entry("Nha Trang", "tháng 1–8 (ít mưa)"),
            Map.entry("Hội An", "tháng 2–8 (trời nắng, ít mưa)"),
            Map.entry("Hoi An", "tháng 2–8 (trời nắng, ít mưa)"),
            Map.entry("Sapa", "tháng 9–10 (lúa chín) hoặc tháng 12–2 (có tuyết/sương)"),
            Map.entry("Sa Pa", "tháng 9–10 (lúa chín) hoặc tháng 12–2 (có tuyết/sương)"),
            Map.entry("Ninh Bình", "tháng 4–6 (xanh, lúa) hoặc 9–10"),
            Map.entry("Ninh Binh", "tháng 4–6 (xanh, lúa) hoặc 9–10"),
            Map.entry("Phan Thiết", "tháng 11–4 (ít mưa)"),
            Map.entry("Phan Thiet", "tháng 11–4 (ít mưa)"),
            Map.entry("Hạ Long", "tháng 10–4 (mát, ít mưa)"),
            Map.entry("Ha Long", "tháng 10–4 (mát, ít mưa)")
    );

    private static String getBestPeriodByDestination(String destination) {
        if (destination == null) return null;
        String d = destination.trim();
        return BEST_PERIOD_MAP.get(d);
    }

    private void extractSlotsFromKeywords(String content, Map<String, Object> slots) {
        if (content == null) return;
        String lower = content.toLowerCase();
        String kw = extractSearchKeyword(content);
        if (kw != null && (slots.get("destination") == null || lower.contains(kw.toLowerCase())))
            slots.put("destination", kw);
        Pattern budgetPattern = Pattern.compile("(\\d+)\\s*(triệu|tr)");
        Matcher budgetMatcher = budgetPattern.matcher(content);
        if (budgetMatcher.find() && !slots.containsKey("budget")) {
            try {
                int num = Integer.parseInt(budgetMatcher.group(1));
                slots.put("budget", num + " triệu");
                slots.put("budget_min", num - 1);
                slots.put("budget_max", num + 1);
            } catch (NumberFormatException ignored) {}
        }
        Pattern daysPattern = Pattern.compile("(\\d+)\\s*ngày\\s*(\\d+)?\\s*đêm?");
        Matcher daysMatcher = daysPattern.matcher(lower);
        if (daysMatcher.find() && slots.get("duration_days") == null) {
            try {
                int d = Integer.parseInt(daysMatcher.group(1));
                slots.put("duration_days", d);
                slots.put("duration", d);
            } catch (NumberFormatException ignored) {}
        }
        if (lower.contains("miền trung") || lower.contains("mien trung")) slots.putIfAbsent("region", "miền Trung");
        if (lower.contains("miền bắc") || lower.contains("mien bac")) slots.putIfAbsent("region", "miền Bắc");
        if (lower.contains("miền nam") || lower.contains("mien nam")) slots.putIfAbsent("region", "miền Nam");
        if (lower.contains("yên tĩnh") || lower.contains("chữa lành")) slots.putIfAbsent("vibe", "yên tĩnh, chữa lành");
        if (lower.contains("trekking")) slots.putIfAbsent("travel_style", "trekking");
    }

    private List<String> parseEntitiesList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private static String fillTemplate(String template, Map<String, Object> slots) {
        if (template == null) return "";
        String out = template;
        Pattern p = Pattern.compile("\\{([^}]+)}");
        Matcher m = p.matcher(template);
        while (m.find()) {
            String key = m.group(1);
            Object val = slots != null ? slots.get(key) : null;
            String replacement = val != null ? val.toString() : "...";
            out = out.replace("{" + key + "}", replacement);
        }
        return out;
    }
}
