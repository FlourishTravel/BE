package com.flourishtravel.config;

import com.flourishtravel.domain.content.entity.SiteContent;
import com.flourishtravel.domain.content.repository.SiteContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Order(90)
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class SiteContentSeeder implements CommandLineRunner {

    private final SiteContentRepository siteContentRepository;

    @Override
    public void run(String... args) {
        Instant now = Instant.now();
        if (siteContentRepository.count() == 0) {
            log.info("Seeding site content...");
            seedNews(now);
            seedStories(now);
            seedCareers(now);
            seedHelp(now);
        }
        if (siteContentRepository.findAdmin("guide").isEmpty()) {
            log.info("Seeding travel guide articles...");
            seedGuide(now);
        }
    }

    private void seedGuide(Instant now) {
        save("guide", "pack-light-5-7-days", "Chuẩn bị vali cho tour trải nghiệm 5–7 ngày",
                "Mách bạn cách sắp xếp đồ gọn nhẹ và những món không nên mang khi đi tour nhóm.",
                "Chọn trang phục lớp lớp, giày đi bộ thoải mái, sạc dự phòng và túi chống nước. Tránh mang quá nhiều đồ trang sức hoặc vali quá nặng khi tour có nhiều chuyển điểm.",
                "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?auto=format&fit=crop&w=600&q=80", "Chuẩn bị", 1, now);
        save("guide", "thailand-etiquette", "Văn hóa và phép lịch sự khi du lịch Thái Lan",
                "Những điều nên biết trước khi tham gia tour tại Bangkok và Pattaya.",
                "Khi vào chùa cần ăn mặc kín đáo, cởi giày và giữ im lặng. Không chạm đầu người Thái. Để lại tiền tip vừa phải tại nhà hàng và khách sạn.",
                "https://images.unsplash.com/photo-1528183429752-a97d0bf99b5a?auto=format&fit=crop&w=600&q=80", "Văn hóa", 2, now);
        save("guide", "stay-healthy-abroad", "Giữ sức khỏe khi đi tour nước ngoài",
                "Mẹo ăn uống an toàn, chống say tàu xe và xử lý cấp cứu nhẹ.",
                "Uống đủ nước, mang thuốc cá nhân và bảo hiểm du lịch. Báo HDV ngay nếu có dị ứng thực phẩm hoặc cần hỗ trợ y tế.",
                "https://images.unsplash.com/photo-1576091160399-112ba8d25d1d?auto=format&fit=crop&w=600&q=80", "Sức khỏe", 3, now);
    }

    private void seedNews(Instant now) {
        save("news", "flourish-slow-living-thailand", "Flourish ra mắt chuỗi tour Sống chậm tại Thái Lan",
                "Các tour 5–7 ngày tập trung trải nghiệm địa phương, homestay và ẩm thực.",
                "Flourish Tourism giới thiệu chuỗi tour wellness và slow travel tại Bangkok – Pattaya, hướng tới du khách muốn tạm rời nhịp sống nhanh.",
                "https://images.unsplash.com/photo-1508009603885-027cf6d0bf6b?auto=format&fit=crop&w=600&q=80", null, 1, now);
        save("news", "partnership-costa-rica", "Hợp tác cộng đồng bản địa Costa Rica",
                "Trải nghiệm rừng nhiệt đới bền vững cùng hướng dẫn viên địa phương.",
                "Flourish ký kết hợp tác với các gia đình địa phương tại Monteverde để mang đến trải nghiệm chân thực.",
                "https://images.unsplash.com/photo-1448375240586-882707db888b?auto=format&fit=crop&w=600&q=80", null, 2, now);
        save("news", "cancellation-policy-2025", "Chính sách hủy tour và hoàn tiền mới",
                "Cập nhật từ 01/03/2025 — minh bạch và linh hoạt hơn.",
                "Chính sách hủy tour được cập nhật để rõ ràng hơn, hỗ trợ khách trong trường hợp bất khả kháng. Xem chi tiết tại trang Chính sách hủy.",
                "https://images.unsplash.com/photo-1488646953014-85cb44e25828?auto=format&fit=crop&w=600&q=80", null, 3, now);
    }

    private void seedStories(Instant now) {
        save("story", "healing-bangkok-retreat", "Hành trình healing giữa lòng Bangkok",
                "Khách Flourish chia sẻ về 5 ngày tĩnh tại spa và làng nghề.",
                "Sau tour Bangkok Healing, chị Lan đã tìm lại nhịp sống cân bằng nhờ yoga buổi sáng và workshop thủ công địa phương.",
                "https://images.unsplash.com/photo-1552465011-b4e21bf6e597?auto=format&fit=crop&w=600&q=80", null, 1, now);
        save("story", "family-pattaya-adventure", "Gia đình 4 người khám phá Pattaya khác biệt",
                "Trải nghiệm biển đảo và văn hóa địa phương cùng HDV Flourish.",
                "Gia đình anh Minh đánh giá cao lịch trình linh hoạt và điểm dừng ít đông đúc của Flourish.",
                "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=600&q=80", null, 2, now);
    }

    private void seedCareers(Instant now) {
        save("career", "tour-guide-bangkok", "Hướng dẫn viên tour Bangkok – Pattaya",
                "Toàn thời gian · TP.HCM / Bangkok",
                "Tìm HDV năng động, thành thạo tiếng Việt – Anh, yêu thích văn hóa Thái. Ưu tiên có chứng chỉ hướng dẫn.",
                null, "Hướng dẫn viên", 1, now);
        save("career", "tour-operations", "Chuyên viên điều hành tour",
                "Toàn thời gian · TP.HCM",
                "Phối hợp lịch khởi hành, HDV và nhà cung cấp địa phương. Kinh nghiệm du lịch outbound là lợi thế.",
                null, "Vận hành", 2, now);
        save("career", "digital-marketing", "Marketing Digital Du lịch",
                "Full-time / Hybrid",
                "Xây dựng chiến dịch cho tour trải nghiệm, quản lý social và nội dung đa kênh.",
                null, "Marketing", 3, now);
    }

    private void seedHelp(Instant now) {
        save("help", "how-to-book-tour", "Cách đặt tour trên Flourish",
                "Hướng dẫn chọn tour, session và thanh toán MoMo/chuyển khoản.",
                "1. Chọn tour tại Danh sách tour. 2. Chọn ngày khởi hành. 3. Điền thông tin và thanh toán. 4. Theo dõi tại Chuyến đi của tôi.",
                null, "Đặt tour", 1, now);
        save("help", "refund-policy-help", "Chính sách hủy và hoàn tiền",
                "Điều kiện hủy tour và quy trình yêu cầu hoàn tiền.",
                "Khách có thể yêu cầu hoàn tiền trong mục chi tiết booking sau khi đủ điều kiện. Admin xử lý trong 3–5 ngày làm việc.",
                null, "Thanh toán", 2, now);
        save("help", "group-chat-guide", "Chat đoàn và liên lạc HDV",
                "Cách tham gia nhóm chat sau khi thanh toán.",
                "Sau khi booking được xác nhận, mở Chuyến đi của tôi → Chat đoàn để nhắn tin với HDV và điều hành.",
                null, "Trong tour", 3, now);
    }

    private void save(String type, String slug, String title, String summary, String body,
                      String imageUrl, String category, int sortOrder, Instant publishedAt) {
        siteContentRepository.save(SiteContent.builder()
                .type(type)
                .slug(slug)
                .title(title)
                .summary(summary)
                .body(body)
                .imageUrl(imageUrl)
                .category(category)
                .published(true)
                .sortOrder(sortOrder)
                .publishedAt(publishedAt)
                .build());
    }
}
