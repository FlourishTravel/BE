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
    @Order(3)
    @Transactional
    public void seed() {
        if (policyFaqRepository.count() > 0) {
            log.debug("Policy FAQ already exist, skip seed");
            return;
        }

        List<PolicyFaq> list = List.of(
                PolicyFaq.builder().topicKey("cancellation").title("Chính sách hủy tour")
                        .content("Khách hủy trước 7 ngày so với ngày khởi hành: hoàn 100% chi phí. Trước 3–7 ngày: hoàn 50%. Trước 1–3 ngày: hoàn 30%. Sau đó hoặc vắng mặt: không hoàn tiền. Một số tour có chính sách riêng, vui lòng xem tại trang chi tiết tour.")
                        .sortOrder(1).build(),
                PolicyFaq.builder().topicKey("change_date").title("Đổi ngày / đổi tour")
                        .content("Khách được đổi sang ngày khác hoặc tour khác (cùng giá trị) một lần miễn phí nếu thông báo trước 5 ngày. Đổi lần 2 trở đi hoặc đổi trong vòng 5 ngày có thể phí 10% giá tour. Đổi sang tour giá cao hơn: bù thêm phần chênh lệch.")
                        .sortOrder(2).build(),
                PolicyFaq.builder().topicKey("refund").title("Hoàn tiền")
                        .content("Tiền hoàn sẽ được chuyển về tài khoản/ ví đã thanh toán trong vòng 5–7 ngày làm việc sau khi yêu cầu được duyệt. Nếu quý khách thanh toán bằng tiền mặt tại văn phòng, hoàn trả bằng chuyển khoản hoặc tiền mặt theo thỏa thuận.")
                        .sortOrder(3).build(),
                PolicyFaq.builder().topicKey("payment").title("Thanh toán")
                        .content("Chấp nhận thanh toán qua ví MoMo, chuyển khoản ngân hàng, thẻ tín dụng/ghi nợ và tiền mặt tại văn phòng. Tour cần được thanh toán đủ hoặc đặt cọc ít nhất 30% trong vòng 24h sau khi đặt để giữ chỗ.")
                        .sortOrder(4).build(),
                PolicyFaq.builder().topicKey("children").title("Trẻ em đi cùng")
                        .content("Trẻ dưới 5 tuổi (không chiếm chỗ, không ăn riêng): miễn phí. Trẻ 5–11 tuổi: thường 50–75% giá người lớn tùy tour. Từ 12 tuổi: giá như người lớn. Chi tiết từng tour xem tại trang sản phẩm.")
                        .sortOrder(5).build()
        );
        policyFaqRepository.saveAll(list);
        log.info("Seeded {} policy/FAQ entries", list.size());
    }
}
