package com.flourishtravel.domain.chatbot.service;

import com.flourishtravel.domain.chatbot.dto.ChatbotRequest;
import com.flourishtravel.domain.chatbot.dto.ChatbotResponse;
import com.flourishtravel.domain.chatbot.entity.ChatbotTrainingPhrase;
import com.flourishtravel.domain.chatbot.entity.PolicyFaq;
import com.flourishtravel.domain.chatbot.entity.SearchLog;
import com.flourishtravel.domain.chatbot.repository.ChatbotTrainingPhraseRepository;
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
    private final ChatbotTrainingPhraseRepository trainingPhraseRepository;
    private final SearchLogRepository searchLogRepository;

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

            String contentForPolicy = normalizeForMatching(content);
            if (looksLikePolicyQuestion(contentForPolicy)) {
                String policyReply = getPolicyReply(content);
                if (policyReply != null) {
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

        return ChatbotResponse.builder()
                .reply(reply)
                .tours(tours)
                .quickReplies(quickReplies)
                .state(state)
                .build();
    }

    private List<ChatbotResponse.TourCard> searchToursWithDuration(String destination, BigDecimal minPrice, BigDecimal maxPrice, Integer durationDays, int limit) {
        var page = tourRepository.search(
                destination != null && !destination.isBlank() ? destination : null,
                minPrice, maxPrice, null, null,
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
        return list.stream().limit(limit).map(this::toTourCard).collect(Collectors.toList());
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
        if (lower.contains("miền trung") || lower.contains("mien trung")) return "Đà Nẵng";
        if (lower.contains("miền bắc") || lower.contains("mien bac")) return "Hạ Long";
        if (lower.contains("miền nam") || lower.contains("mien nam")) return "Phú Quốc";
        if (lower.contains("hà giang") || lower.contains("ha giang")) return "Hà Giang";
        if (lower.contains("biển") || lower.contains("bãi") || lower.contains("tour biển")) return "biển";
        if (lower.contains("tour") && content.length() > 3) return "tour";
        return content.length() > 2 ? content : null;
    }
}
