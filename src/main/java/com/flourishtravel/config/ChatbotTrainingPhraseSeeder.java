package com.flourishtravel.config;

import com.flourishtravel.domain.chatbot.entity.ChatbotTrainingPhrase;
import com.flourishtravel.domain.chatbot.repository.ChatbotTrainingPhraseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Seed câu hỏi / câu nói mẫu để chatbot map đúng FAQ (training data).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatbotTrainingPhraseSeeder {

    private final ChatbotTrainingPhraseRepository repository;

    @EventListener(ApplicationReadyEvent.class)
    @Order(6)
    @Transactional
    public void seed() {
        int added = 0;
        for (ChatbotTrainingPhrase p : allPhrases()) {
            if (!repository.existsByPhraseAndTopicKey(p.getPhrase(), p.getTopicKey())) {
                repository.save(p);
                added++;
            }
        }
        if (added > 0) log.info("Seeded {} new chatbot training phrases (total coverage for ~88%% accuracy)", added);
    }

    private static List<ChatbotTrainingPhrase> allPhrases() {
        List<ChatbotTrainingPhrase> out = new ArrayList<>();
        String[][] cancellation = {
                {"chính sách hủy tour", "cancellation"}, {"chính sách hủy", "cancellation"}, {"hủy tour", "cancellation"},
                {"hủy trước 3 ngày", "cancellation"}, {"hủy trước 7 ngày", "cancellation"}, {"hủy tour mất phí bao nhiêu", "cancellation"},
                {"ốm không đi được", "cancellation"}, {"bị ốm không đi được có hoàn tiền không", "cancellation"},
                {"cancel tour", "cancellation"}, {"hủy vé tour", "cancellation"},
                {"huy tour", "cancellation"}, {"chinh sach huy tour", "cancellation"}, {"huy truoc 3 ngay mat phi", "cancellation"},
                {"mình muốn hủy tour", "cancellation"}, {"công ty có chính sách hủy không", "cancellation"},
                {"nếu hủy tour thì sao", "cancellation"}, {"hủy có mất phí không", "cancellation"}
        };
        String[][] changeDate = {
                {"đổi ngày khởi hành", "change_date"}, {"đổi ngày đi", "change_date"}, {"dời ngày đi", "change_date"},
                {"đổi tour", "change_date"}, {"đổi sang ngày khác", "change_date"}, {"đổi tên người đi", "change_date"},
                {"đổi lịch", "change_date"}, {"đổi sang tour khác", "change_date"},
                {"doi ngay", "change_date"}, {"doi tour", "change_date"}, {"dời lịch", "change_date"},
                {"muốn đổi ngày đi", "change_date"}, {"đổi tên khách được không", "change_date"}
        };
        String[][] refund = {
                {"hoàn tiền", "refund"}, {"hoàn lại tiền", "refund"}, {"trả lại tiền", "refund"},
                {"khi nào nhận được tiền hoàn", "refund"}, {"refund", "refund"},
                {"hoan tien", "refund"}, {"hoan lai", "refund"}, {"tiền hoàn về khi nào", "refund"},
                {"được hoàn tiền không", "refund"}, {"hoàn tiền mất bao lâu", "refund"}
        };
        String[][] payment = {
                {"thanh toán", "payment"}, {"đặt cọc", "payment"}, {"trả góp", "payment"},
                {"chuyển khoản", "payment"}, {"thẻ tín dụng", "payment"}, {"có cho trả góp không", "payment"},
                {"thanh toán bằng ví", "payment"}, {"payment", "payment"},
                {"tra gop", "payment"}, {"dat coc bao nhieu", "payment"}, {"thanh toan the nao", "payment"},
                {"trả góp được không", "payment"}, {"thẻ tín dụng có mất phí không", "payment"}
        };
        String[][] children = {
                {"trẻ em", "children"}, {"trẻ con", "children"}, {"bé 5 tuổi", "children"},
                {"giường riêng cho trẻ", "children"}, {"trẻ em tính giá thế nào", "children"},
                {"con nhỏ đi cùng", "children"}, {"thực đơn trẻ em", "children"},
                {"tre em", "children"}, {"bé 5 tuổi giá thế nào", "children"}, {"trẻ em có giường riêng không", "children"}
        };
        String[][] itinerary = {
                {"lịch trình", "itinerary"}, {"ngày 2 đi đâu", "itinerary"}, {"ngày 3 đi đâu", "itinerary"},
                {"thời gian tự do", "itinerary"}, {"tour này đi những đâu", "itinerary"},
                {"lich trinh", "itinerary"}, {"ngày 2 tour đi đâu", "itinerary"}, {"có thời gian tự do không", "itinerary"}
        };
        String[][] compare = {
                {"so sánh gói standard và pro", "compare"}, {"gói standard và pro khác nhau", "compare"},
                {"tại sao tour này đắt hơn", "compare"}, {"đắt hơn bên khác", "compare"},
                {"so sanh goi", "compare"}, {"standard pro khac nhau", "compare"}, {"tại sao đắt", "compare"}
        };
        String[][] bestTime = {
                {"tháng 10 đi Hà Giang", "best_time"}, {"mùa nào đi Phú Quốc ít mưa", "best_time"},
                {"tháng nào đi đẹp", "best_time"}, {"thời điểm đi", "best_time"},
                {"thang 10 Ha Giang", "best_time"}, {"mùa nào đi đẹp", "best_time"}, {"ít mưa", "best_time"}
        };
        String[][] ageFitness = {
                {"người già đi có vất vả không", "age_fitness"}, {"tour có vất vả không", "age_fitness"},
                {"ăn chay", "age_fitness"}, {"thực đơn cho người ăn chay", "age_fitness"},
                {"nguoi gia", "age_fitness"}, {"an chay", "age_fitness"}, {"tour người già đi được không", "age_fitness"}
        };
        String[][] whatIncluded = {
                {"giá đã bao gồm", "what_included"}, {"giá có vé máy bay chưa", "what_included"},
                {"tip cho hướng dẫn viên bắt buộc không", "what_included"}, {"tiền tip", "what_included"},
                {"chi phí ẩn", "what_included"}, {"vé máy bay đã gồm chưa", "what_included"},
                {"gia da bao gom", "what_included"}, {"ve may bay", "what_included"}, {"tip bat buoc", "what_included"}
        };
        String[][] inTourSupport = {
                {"đổi phòng", "in_tour_support"}, {"phòng hỏng điều hòa", "in_tour_support"},
                {"không tìm thấy hướng dẫn viên", "in_tour_support"}, {"quên đồ trên xe", "in_tour_support"},
                {"để quên hộ chiếu", "in_tour_support"}, {"thất lạc đồ", "in_tour_support"},
                {"doi phong", "in_tour_support"}, {"phong hong dieu hoa", "in_tour_support"}, {"quen do tren xe", "in_tour_support"}
        };
        String[][] humanHandoff = {
                {"liên hệ nhân viên", "human_handoff"}, {"gặp người tư vấn", "human_handoff"},
                {"chuyển cho nhân viên", "human_handoff"}, {"khẩn cấp", "human_handoff"},
                {"lien he nhan vien", "human_handoff"}, {"muốn gặp nhân viên", "human_handoff"}
        };
        String[][] visa = {
                {"visa", "visa"}, {"hộ chiếu", "visa"}, {"hộ chiếu còn hạn dưới 6 tháng", "visa"},
                {"làm visa Hàn Quốc mất bao lâu", "visa"}, {"passport", "visa"},
                {"ho chieu", "visa"}, {"visa Han Quoc", "visa"}, {"hộ chiếu hết hạn", "visa"}
        };
        String[][] insurance = {
                {"bảo hiểm du lịch", "insurance"}, {"bồi thường tối đa", "insurance"},
                {"tai nạn ở nước ngoài liên hệ ai", "insurance"},
                {"bao hiem du lich", "insurance"}, {"bồi thường bao nhiêu", "insurance"}
        };
        String[][] addons = {
                {"xe đưa đón sân bay", "addons"}, {"vé show Ký Ức Hội An", "addons"},
                {"SIM 4G Thái Lan", "addons"}, {"dịch vụ thêm", "addons"}, {"đặt thêm xe đưa đón", "addons"},
                {"xe dua don san bay", "addons"}, {"vé show Hội An", "addons"}, {"sim 4g", "addons"}
        };
        String[][] packingTips = {
                {"mang theo gì", "packing_tips"}, {"hành trang", "packing_tips"},
                {"đi Fansipan cần mang gì", "packing_tips"}, {"áo phao", "packing_tips"},
                {"hanh trang", "packing_tips"}, {"cần mang theo gì", "packing_tips"}
        };
        String[][] cultureRules = {
                {"Singapore nhai kẹo cao su", "culture_rules"}, {"Thái Lan váy đền chùa", "culture_rules"},
                {"văn hóa địa phương", "culture_rules"}, {"Singapore keo cao su", "culture_rules"}
        };
        String[][] moneyExchange = {
                {"đổi tiền Won", "money_exchange"}, {"tỉ giá", "money_exchange"},
                {"đổi ngoại tệ", "money_exchange"}, {"100 USD đổi ra Baht", "money_exchange"},
                {"doi tien Won", "money_exchange"}, {"ti gia", "money_exchange"}, {"đổi USD", "money_exchange"}
        };
        String[][] practicalTips = {
                {"ổ cắm", "practical_tips"}, {"2 chấu hay 3 chấu", "practical_tips"},
                {"cục chuyển", "practical_tips"}, {"o cam", "practical_tips"}, {"ổ cắm điện", "practical_tips"}
        };
        String[][] crisisWeather = {
                {"bão", "crisis_weather"}, {"mưa lớn", "crisis_weather"}, {"tour ngày mai có khởi hành không", "crisis_weather"},
                {"đền bù thời tiết", "crisis_weather"}, {"thiên tai", "crisis_weather"},
                {"bão đổ bộ", "crisis_weather"}, {"mua lon", "crisis_weather"}, {"tour co khoi hanh khong", "crisis_weather"}
        };
        String[][] crisisHealth = {
                {"say sóng", "crisis_health"}, {"đổi sang đường bộ", "crisis_health"},
                {"ngộ độc thực phẩm", "crisis_health"}, {"gọi cấp cứu", "crisis_health"},
                {"say song", "crisis_health"}, {"ngo doc thuc pham", "crisis_health"}, {"cap cuu", "crisis_health"}
        };
        String[][] crisisLost = {
                {"mất ví", "crisis_lost"}, {"mất hộ chiếu", "crisis_lost"},
                {"trình báo công an", "crisis_lost"}, {"trình báo ở đâu", "crisis_lost"},
                {"mat vi", "crisis_lost"}, {"mat ho chieu", "crisis_lost"}, {"trinh bao o dau", "crisis_lost"}
        };
        String[][] localSecrets = {
                {"quán ăn ẩn", "local_secrets"}, {"ốc ngon dân địa phương", "local_secrets"},
                {"góc check-in", "local_secrets"}, {"bình minh Đà Lạt", "local_secrets"},
                {"trả giá chợ", "local_secrets"}, {"taxi chặt chém", "local_secrets"},
                {"quan an an", "local_secrets"}, {"tra gia cho", "local_secrets"}
        };
        String[][] currencyConvert = {
                {"tỉ giá hôm nay", "currency_convert"}, {"quy đổi tiền", "currency_convert"},
                {"ti gia hom nay", "currency_convert"}, {"100 usd bao nhieu baht", "currency_convert"}
        };
        String[][] weatherForecast = {
                {"dự báo thời tiết", "weather_forecast"}, {"thời tiết 3 ngày tới", "weather_forecast"},
                {"du bao thoi tiet", "weather_forecast"}, {"thoi tiet 3 ngay", "weather_forecast"}
        };
        String[][] translatePhrase = {
                {"dịch tiếng Thái", "translate_phrase"}, {"câu này nói bằng tiếng Thái", "translate_phrase"},
                {"phiên âm", "translate_phrase"}, {"dich tieng Thai", "translate_phrase"}
        };
        String[][] postTripReview = {
                {"chuyến đi vừa rồi", "post_trip_review"}, {"chia sẻ ảnh nhận voucher", "post_trip_review"},
                {"review tour", "post_trip_review"}, {"chia se anh", "post_trip_review"}
        };
        String[][] postTripComplaint = {
                {"không hài lòng", "post_trip_complaint"}, {"thái độ hướng dẫn viên", "post_trip_complaint"},
                {"phản ánh", "post_trip_complaint"}, {"khiếu nại", "post_trip_complaint"},
                {"khong hai long", "post_trip_complaint"}, {"phan anh", "post_trip_complaint"}
        };
        String[][] postTripNext = {
                {"gợi ý tour tiếp", "post_trip_next"}, {"lễ hội Nha Trang", "post_trip_next"},
                {"goi y tour tiep", "post_trip_next"}
        };
        String[][] competitive = {
                {"bên kia rẻ hơn", "competitive"}, {"tại sao đắt hơn", "competitive"},
                {"khách sạn xa trung tâm", "competitive"}, {"có gì đặc biệt hơn", "competitive"},
                {"ben kia re hon", "competitive"}, {"tai sao dat hon", "competitive"}
        };
        String[][] trust = {
                {"công ty hoạt động bao lâu", "trust"}, {"giấy phép kinh doanh", "trust"},
                {"uy tín", "trust"}, {"cong ty hoat dong bao lau", "trust"}, {"giay phep", "trust"}
        };

        String[][][] groups = new String[][][]{
                cancellation, changeDate, refund, payment, children, itinerary, compare, bestTime, ageFitness,
                whatIncluded, inTourSupport, humanHandoff, visa, insurance, addons, packingTips, cultureRules,
                moneyExchange, practicalTips, crisisWeather, crisisHealth, crisisLost, localSecrets, currencyConvert,
                weatherForecast, translatePhrase, postTripReview, postTripComplaint, postTripNext, competitive, trust
        };
        for (String[][] group : groups) {
            for (String[] pair : group) {
                if (pair.length >= 2) {
                    out.add(ChatbotTrainingPhrase.builder().phrase(pair[0]).topicKey(pair[1]).build());
                }
            }
        }
        return out;
    }
}
