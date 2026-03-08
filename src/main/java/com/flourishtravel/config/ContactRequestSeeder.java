package com.flourishtravel.config;

import com.flourishtravel.domain.contact.entity.ContactRequest;
import com.flourishtravel.domain.contact.repository.ContactRequestRepository;
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
public class ContactRequestSeeder {

    private final ContactRequestRepository contactRequestRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Order(8)
    @Transactional
    public void seed() {
        if (contactRequestRepository.count() > 0) {
            log.debug("Contact requests already exist, skip seed");
            return;
        }
        List<ContactRequest> list = List.of(
                ContactRequest.builder()
                        .name("Trần Văn A")
                        .email("tranvana@example.com")
                        .phone("0988123456")
                        .message("Tôi muốn tư vấn tour Đà Nẵng 3 ngày 2 đêm cho gia đình 4 người.")
                        .status("new")
                        .build(),
                ContactRequest.builder()
                        .name("Lê Thị B")
                        .email("lethib@example.com")
                        .phone("0909876543")
                        .message("Cho hỏi tour Phú Quốc tháng 6 còn chỗ không? Cần báo giá sớm.")
                        .status("read")
                        .note("Đã gửi báo giá qua email.")
                        .build()
        );
        contactRequestRepository.saveAll(list);
        log.info("Seeded {} contact requests", list.size());
    }
}
