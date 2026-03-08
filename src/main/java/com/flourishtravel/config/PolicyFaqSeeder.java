package com.flourishtravel.config;

import com.flourishtravel.domain.chatbot.entity.PolicyFaq;
import com.flourishtravel.domain.chatbot.repository.PolicyFaqRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PolicyFaqSeeder {

    private final PolicyFaqRepository policyFaqRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Order(5)
    @Transactional
    public void seed() {
        int added = 0;
        for (PolicyFaq faq : allFaqEntries()) {
            if (policyFaqRepository.findByTopicKey(faq.getTopicKey()).isEmpty()) {
                policyFaqRepository.save(faq);
                added++;
            }
        }
        if (added > 0) log.info("Seeded {} new policy/FAQ entries", added);
    }

    private static List<PolicyFaq> allFaqEntries() {
        return List.of(
                PolicyFaq.builder().topicKey("cancellation").title("Chính sách hủy tour")
                        .content("Khách hủy trước 7 ngày so với ngày khởi hành: hoàn 100% chi phí. Trước 3–7 ngày: hoàn 50%. Trước 1–3 ngày: hoàn 30%. Sau đó hoặc vắng mặt: không hoàn tiền. Trường hợp bất khả kháng (ốm nặng, tai nạn có giấy tờ): xem xét từng trường hợp. Chi tiết từng tour xem tại trang sản phẩm.")
                        .sortOrder(1).build(),
                PolicyFaq.builder().topicKey("change_date").title("Đổi ngày / đổi tour / đổi thông tin")
                        .content("Đổi ngày khởi hành hoặc đổi tour (cùng giá trị): một lần miễn phí nếu thông báo trước 5 ngày. Đổi lần 2 hoặc trong vòng 5 ngày: có thể phí 10% giá tour. Đổi tên người đi: thông báo trước 24h, không phí. Đổi sang tour giá cao hơn: bù thêm chênh lệch. Chi tiết xem trang chính sách.")
                        .sortOrder(2).build(),
                PolicyFaq.builder().topicKey("refund").title("Hoàn tiền")
                        .content("Tiền hoàn chuyển về tài khoản/ví đã thanh toán trong 5–7 ngày làm việc sau khi yêu cầu được duyệt. Thanh toán tiền mặt tại văn phòng: hoàn trả chuyển khoản hoặc tiền mặt theo thỏa thuận.")
                        .sortOrder(3).build(),
                PolicyFaq.builder().topicKey("payment").title("Thanh toán")
                        .content("Chấp nhận: ví MoMo, chuyển khoản ngân hàng, thẻ tín dụng/ghi nợ, tiền mặt tại văn phòng. Thanh toán thẻ quốc tế: có thể phí chuyển đổi theo ngân hàng phát hành thẻ. Trả góp: áp dụng một số tour qua đối tác tài chính, xem tại trang thanh toán. Tour cần đặt cọc ít nhất 30% trong 24h để giữ chỗ.")
                        .sortOrder(4).build(),
                PolicyFaq.builder().topicKey("children").title("Trẻ em & Giường riêng")
                        .content("Trẻ dưới 5 tuổi (không chiếm chỗ, không ăn riêng): miễn phí. Trẻ 5–11 tuổi: thường 50–75% giá người lớn tùy tour. Từ 12 tuổi: giá như người lớn. Trẻ em có thể được giường riêng nếu đặt option (phụ thu). Thực đơn trẻ em/ăn chay: một số tour có, xem chi tiết từng tour hoặc ghi chú khi đặt.")
                        .sortOrder(5).build(),
                PolicyFaq.builder().topicKey("itinerary").title("Lịch trình & Thời gian tự do")
                        .content("Lịch trình chi tiết từng ngày (điểm tham quan, bữa ăn, nghỉ đêm) có trong trang chi tiết tour và file gửi sau khi đặt. Nhiều tour có thời gian tự do buổi tối hoặc một phần ngày; phần cố định có hướng dẫn viên. Bạn có thể hỏi 'Tour [tên] ngày 2 đi đâu?' trên trang đó hoặc nhắn cho bộ phận tư vấn.")
                        .sortOrder(10).build(),
                PolicyFaq.builder().topicKey("compare").title("So sánh gói / Giá tour")
                        .content("Các gói (Standard, Pro, Cao cấp) khác nhau chủ yếu ở hạng phòng, bữa ăn và dịch vụ kèm theo (ví dụ spa, vé thắng cảnh). So sánh chi tiết có trên trang từng tour. Giá FlourishTravel đã bao gồm theo mô tả từng gói; chi phí không bao gồm (vé máy bay riêng, tip tự nguyện...) được ghi rõ. Tip cho HDV/tài xế: không bắt buộc, tùy lòng khách.")
                        .sortOrder(11).build(),
                PolicyFaq.builder().topicKey("best_time").title("Thời điểm đi / Thời tiết")
                        .content("Thời điểm đẹp tùy điểm đến: ví dụ Hà Giang đẹp tháng 9–10 (mùa lúa), Phú Quốc ít mưa khoảng tháng 11–4. Mỗi tour có gợi ý 'Nên đi khi nào' trên trang chi tiết. Bạn có thể hỏi 'Tháng X đi Y có đẹp không?' – mình gợi ý chung; để chắc chắn nên xem trang tour hoặc liên hệ tư vấn.")
                        .sortOrder(12).build(),
                PolicyFaq.builder().topicKey("age_fitness").title("Độ tuổi & Sức khỏe")
                        .content("Tour có mức độ vận động khác nhau: một số phù hợp người lớn tuổi (ít trek, xe đưa đón), một số cần thể lực tốt. Trên trang chi tiết tour có mục 'Đối tượng' hoặc 'Lưu ý'. Nếu bạn lo người già/trẻ nhỏ có vất vả không, hãy xem tour đó hoặc nhắn 'Tour [tên] người già đi có vất vả không?' – mình gợi ý theo mô tả tour.")
                        .sortOrder(13).build(),
                PolicyFaq.builder().topicKey("what_included").title("Giá đã bao gồm / Chi phí ẩn")
                        .content("Giá tour đã bao gồm: theo mục 'Bao gồm' trên trang chi tiết (thường có vận chuyển, phòng, bữa ăn theo lịch, vé tham quan trong chương trình, HDV). Không bao gồm: thường là đồ uống cá nhân, chi phí cá nhân, bảo hiểm (nếu không ghi trong gói). Vé máy bay: chỉ có khi tour ghi 'đã bao gồm vé máy bay'. Tip HDV/tài xế: tự nguyện, không bắt buộc.")
                        .sortOrder(14).build(),
                PolicyFaq.builder().topicKey("in_tour_support").title("Hỗ trợ trong chuyến đi (đổi phòng, quên đồ)")
                        .content("Trong chuyến đi, bạn liên hệ số hotline trên voucher/email xác nhận để được hỗ trợ 24/7. Vấn đề tại chỗ (phòng hỏng điều hòa, không gặp HDV ở điểm hẹn): gọi hotline để đổi phòng hoặc xác nhận điểm hẹn ngay. Quên đồ trên xe/khách sạn: báo HDV hoặc hotline để hỗ trợ liên hệ tài xế/khách sạn. FlourishTravel sẽ nỗ lực hỗ trợ trong phạm vi có thể.")
                        .sortOrder(15).build(),
                PolicyFaq.builder().topicKey("visa").title("Visa & Hộ chiếu")
                        .content("Tour nội địa: chỉ cần CCCD/CMND. Tour quốc tế: cần hộ chiếu còn hạn (thường tối thiểu 6 tháng). Visa: tùy nước đến – một số miễn visa với công dân VN (Thái, Singapore, Hàn...), một số cần xin. Thời gian làm visa tùy nước (vd Hàn Quốc thường vài ngày đến vài tuần). Bạn nên kiểm tra trang tour outbound hoặc liên hệ tư vấn trước khi đặt.")
                        .sortOrder(16).build(),
                PolicyFaq.builder().topicKey("insurance").title("Bảo hiểm du lịch")
                        .content("Một số tour đã bao gồm bảo hiểm du lịch nội địa; tour quốc tế thường yêu cầu hoặc khuyến nghị mua bảo hiểm. Mức bồi thường và điều khoản xem trong hợp đồng/ghi chú tour. Tai nạn ở nước ngoài: liên hệ số khẩn cấp trên thẻ bảo hiểm và báo cho FlourishTravel hotline để hỗ trợ thủ tục.")
                        .sortOrder(17).build(),
                PolicyFaq.builder().topicKey("human_handoff").title("Chuyển nhân viên hỗ trợ")
                        .content("Với tình huống khẩn cấp, phức tạp hoặc khi bạn cần trao đổi trực tiếp với nhân viên, vui lòng gọi hotline FlourishTravel (số trên website) hoặc để lại tin nhắn qua form Liên hệ. Nhân viên sẽ phản hồi trong giờ hành chính; trường hợp khẩn cấp trong chuyến đi dùng số hotline 24/7 in trên voucher.")
                        .sortOrder(20).build(),
                // Nhóm Tư vấn theo Gu, Upsell, Câu hỏi bên lề
                PolicyFaq.builder().topicKey("addons").title("Dịch vụ đi kèm (xe đưa đón, vé show, SIM)")
                        .content("FlourishTravel có dịch vụ bổ sung: xe đưa đón sân bay, vé xem show (vd Ký Ức Hội An), SIM/4G du lịch quốc tế. Bạn có thể chọn khi đặt tour hoặc nhắn 'đặt thêm xe đưa đón' / 'vé show Ký Ức Hội An' / 'SIM Thái Lan' – nhân viên sẽ báo giá và xác nhận. Một số tour đã gói sẵn, xem tại trang chi tiết.")
                        .sortOrder(30).build(),
                PolicyFaq.builder().topicKey("packing_tips").title("Gợi ý hành trang")
                        .content("Tùy điểm đến và mùa: đi Fansipan/vùng lạnh nên mang áo ấm, áo phao, miếng dán giữ nhiệt; đi biển cần kem chống nắng, đồ bơi. Trang chi tiết tour có mục 'Lưu ý mang theo'. Bạn hỏi 'Đi [địa điểm] cần mang gì?' mình gợi ý chung; chi tiết xem trên tour hoặc liên hệ tư vấn.")
                        .sortOrder(31).build(),
                PolicyFaq.builder().topicKey("culture_rules").title("Văn hóa & Quy định địa phương")
                        .content("Mỗi nước/địa điểm có quy định riêng: ví dụ Singapore cấm nhai kẹo cao su nơi công cộng; Thái Lan vào đền chùa nên mặc kín (váy quần qua gối, không hở vai). HDV tour sẽ nhắc khi đi; bạn cũng có thể xem mục 'Lưu ý' trên trang tour hoặc hỏi trước khi đặt.")
                        .sortOrder(32).build(),
                PolicyFaq.builder().topicKey("money_exchange").title("Đổi ngoại tệ")
                        .content("Đổi ngoại tệ (Won, Bath, USD...) nên so sánh tỉ giá tại ngân hàng, tiệm vàng uy tín hoặc sân bay. FlourishTravel không cung cấp dịch vụ đổi tiền; HDV có thể gợi ý địa điểm đổi an toàn tại điểm đến. Bạn nên đổi một phần trước chuyến đi để tiện chi tiêu.")
                        .sortOrder(33).build(),
                PolicyFaq.builder().topicKey("practical_tips").title("Ổ cắm điện & Tiện ích")
                        .content("Khách sạn Việt Nam thường dùng ổ cắm 2 chấu (220V); một số phòng có ổ 3 chấu. Đi tour quốc tế: tùy nước (Thái, Singapore thường 2 chấu 220V; Hàn, Nhật có thể khác). Nên mang theo cục chuyển đa năng nếu đi nước ngoài. Chi tiết từng tour có thể ghi trong 'Lưu ý'.")
                        .sortOrder(34).build(),
                PolicyFaq.builder().topicKey("sentiment_apology").title("Xin lỗi & Kết nối nhân viên")
                        .content("FlourishTravel rất tiếc nếu trải nghiệm chưa như mong đợi. Để được hỗ trợ cụ thể về giá, chất lượng dịch vụ hoặc bất kỳ thắc mắc nào, bạn vui lòng gọi hotline hoặc để lại tin nhắn qua form Liên hệ – nhân viên sẽ liên hệ và giải quyết sớm nhất.")
                        .sortOrder(35).build(),
                // Crisis Management
                PolicyFaq.builder().topicKey("crisis_weather").title("Thiên tai / Thời tiết & Tour")
                        .content("Khi có bão, mưa lớn hoặc thiên tai: FlourishTravel theo dõi dự báo và quyết định có hủy/dời tour hay không; khách sẽ được thông báo sớm qua SĐT/email. Tour không khởi hành do bất khả kháng: thường được hoàn tiền hoặc dời sang ngày khác (chi tiết theo từng tour). Trường hợp đang đi mà thời tiết xấu khiến không tham quan được: xử lý theo điều khoản bất khả kháng trong hợp đồng (có thể bù bằng chương trình thay thế hoặc hoàn một phần tùy tour). Bạn gọi hotline để được xác nhận cụ thể cho tour của mình.")
                        .sortOrder(40).build(),
                PolicyFaq.builder().topicKey("crisis_health").title("Sức khỏe / Y tế trong tour")
                        .content("Say sóng nặng muốn đổi sang đường bộ: liên hệ hotline ngay để xem tour có option đổi tuyến hoặc tour khác phù hợp không; tùy thời điểm và chính sách từng tour có thể phí đổi. Trong đoàn có người ngộ độc thực phẩm hoặc cần cấp cứu: gọi ngay số hotline 24/7 trên voucher và 115 (cấp cứu); HDV và công ty sẽ hỗ trợ đưa đến cơ sở y tế gần nhất. Bạn nên ghi số hotline tour và bảo hiểm (nếu có) trước khi đi.")
                        .sortOrder(41).build(),
                PolicyFaq.builder().topicKey("crisis_lost").title("Mất ví / Hộ chiếu / Trình báo")
                        .content("Mất ví tiền hoặc hộ chiếu: (1) Báo ngay cho HDV và hotline FlourishTravel để được hướng dẫn. (2) Trình báo công an: đến đồn/trạm công an gần nơi mất hoặc địa phương nơi bạn đang lưu trú (ở nước ngoài: Đại sứ quán Việt Nam có thể hỗ trợ cấp giấy tạm thời). HDV thường biết địa điểm đồn công an gần nhất trên tuyến; bạn gọi hotline để được chỉ dẫn cụ thể.")
                        .sortOrder(42).build(),
                // Local Secrets
                PolicyFaq.builder().topicKey("local_secrets").title("Gợi ý như người bản địa (quán ẩn, góc chụp, trả giá)")
                        .content("Quán ăn ẩn / dân địa phương hay ăn: HDV tour thường gợi ý trong chuyến đi; bạn cũng có thể hỏi trước khi đi qua hotline hoặc form Liên hệ – bộ phận tư vấn gửi vài gợi ý theo từng điểm đến. Góc check-in ít người (vd bình minh Đà Lạt): mình gợi ý chung (vd hồ Tuyền Lâm, đồi chè); chi tiết từng địa điểm HDV sẽ mách khi đi tour. Chợ trả giá: nhiều chợ địa phương nên trả giá nhẹ, mua nhiều có thể giảm. Taxi tránh chặt chém: ưu tiên Grab/Be hoặc taxi niêm yết giá; đi xe tour thì đã gói trong giá. Bạn cần gợi ý cụ thể cho từng nơi thì nhắn 'Gợi ý quán ăn [địa điểm]' hoặc liên hệ tư vấn.")
                        .sortOrder(43).build(),
                // Digital Utilities
                PolicyFaq.builder().topicKey("currency_convert").title("Quy đổi tiền tệ (USD, Baht...)")
                        .content("Tỉ giá thay đổi theo ngày; FlourishTravel không cung cấp tỉ giá real-time trong chat. Bạn tra cứu tại ngân hàng, app ngân hàng hoặc trang tỉ giá uy tín. Ví dụ tham khảo: 1 USD ≈ 24.000–25.000 VND; 1 VND ≈ 0,0013–0,0014 Baht (tỉ giá có thể khác tại thời điểm bạn đổi). Khi đi tour nước ngoài nên đổi một phần trước và dùng thẻ khi có thể.")
                        .sortOrder(44).build(),
                PolicyFaq.builder().topicKey("weather_forecast").title("Dự báo thời tiết chi tiết")
                        .content("Dự báo thời tiết theo từng giờ/từng ngày mình không có trong hệ thống. Bạn xem trên app/trang thời tiết (vd Weather.com, AccuWeather) hoặc gõ 'dự báo thời tiết [Sa Pa/Đà Lạt/...]'. Mình có thể gợi ý chung theo mùa (vd Sa Pa mùa đông lạnh, mùa hè mát; miền Nam mùa mưa tháng 5–10). Chi tiết từng ngày bạn tra cứu trước ngày đi.")
                        .sortOrder(45).build(),
                PolicyFaq.builder().topicKey("translate_phrase").title("Dịch câu nhanh (tiếng Thái, Trung...)")
                        .content("Mình có thể gợi ý vài câu thường dùng bằng tiếng địa phương (viết + phiên âm), ví dụ tiếng Thái: 'Cái này bao nhiêu tiền?' = 'Rakha thao rai?' (ราคาเท่าไหร่). Để nghe phát âm hoặc dịch câu bất kỳ, bạn dùng app dịch (Google Translate, Microsoft Translator). Trong tour, HDV thường hướng dẫn vài câu cơ bản khi cần.")
                        .sortOrder(46).build(),
                // Post-trip
                PolicyFaq.builder().topicKey("post_trip_review").title("Chia sẻ trải nghiệm & Nhận voucher")
                        .content("FlourishTravel rất vui khi bạn chia sẻ trải nghiệm sau chuyến đi. Bạn có thể gửi review và hình ảnh qua form Liên hệ, fanpage hoặc email – một số chương trình có tặng voucher cho lần đặt tiếp khi chia sẻ ảnh đẹp (chi tiết xem tại mục Khuyến mãi trên website hoặc tin nhắn từ FlourishTravel). Cảm ơn bạn đã đồng hành!")
                        .sortOrder(47).build(),
                PolicyFaq.builder().topicKey("post_trip_complaint").title("Phản ánh / Khiếu nại (HDV, dịch vụ)")
                        .content("Nếu bạn không hài lòng về thái độ hướng dẫn viên hoặc chất lượng dịch vụ, FlourishTravel mong nhận được phản ánh để cải thiện. Bạn vui lòng gửi chi tiết (ngày tour, đoàn, nội dung vấn đề) qua hotline hoặc form Liên hệ/Khiếu nại trên website. Bộ phận chăm sóc khách hàng sẽ xác minh và phản hồi trong thời gian sớm nhất.")
                        .sortOrder(48).build(),
                PolicyFaq.builder().topicKey("post_trip_next").title("Gợi ý hành trình tiếp theo")
                        .content("Dựa trên sở thích và điểm đến bạn đã đi, FlourishTravel có thể gợi ý tour tiếp theo (vd bạn thích biển – tháng sau Nha Trang có lễ hội). Bạn để lại SĐT/email qua form Liên hệ hoặc đăng ký nhận tin để nhận thông tin khuyến mãi và gợi ý tour phù hợp. Bạn cũng có thể nhắn 'Gợi ý tour sau khi đi [địa điểm]' để mình gợi ý chung.")
                        .sortOrder(49).build(),
                // Competitive & Trust
                PolicyFaq.builder().topicKey("competitive").title("So sánh với đối thủ / Giá & Vị trí")
                        .content("FlourishTravel cam kết giá minh bạch và chất lượng dịch vụ. So với các bên khác: mỗi công ty có cách gói tour khác nhau (hạng phòng, bữa ăn, điểm tham quan, bảo hiểm). Nếu bạn thấy tour tương tự rẻ hơn ở nơi khác, có thể do khác hạng phòng hoặc điều kiện hủy/đổi. Vị trí khách sạn: chúng mình chọn theo lịch trình và tiện di chuyển; nếu bạn cần gần trung tâm hơn, có thể xem gói cao cấp hoặc nhắn tư vấn để tìm option phù hợp. Bạn gọi hotline nếu cần so sánh cụ thể một tour.")
                        .sortOrder(50).build(),
                PolicyFaq.builder().topicKey("trust").title("Uy tín & Giấy phép kinh doanh")
                        .content("FlourishTravel hoạt động trong lĩnh vực lữ hành với đầy đủ giấy phép kinh doanh. Thông tin giấy phép lữ hành quốc tế/nội địa và mã số thuế có thể xem tại footer website hoặc yêu cầu qua form Liên hệ. Nếu bạn cần xác nhận trước khi đặt tour, vui lòng gọi hotline – nhân viên sẽ gửi thông tin chính thức.")
                        .sortOrder(51).build()
        );
    }
}
