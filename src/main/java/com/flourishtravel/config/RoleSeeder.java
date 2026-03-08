package com.flourishtravel.config;

import com.flourishtravel.domain.user.entity.Role;
import com.flourishtravel.domain.user.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tạo sẵn 3 role theo FLOWS: TRAVELER, TOUR_GUIDE, ADMIN.
 * SecurityConfig dùng hasRole("ADMIN"), hasAnyRole("ADMIN", "TOUR_GUIDE") → name trong DB nên viết hoa.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RoleSeeder {

    private final RoleRepository roleRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Order(1)
    @Transactional
    public void seedRoles() {
        seed("TRAVELER", "Khách hàng – đặt tour, xem chuyến đi, chat");
        seed("TOUR_GUIDE", "Hướng dẫn viên – lịch công tác, quản đoàn, check-in, ghim tin");
        seed("ADMIN", "Quản trị viên – dashboard, CRUD tour/session/user, giao dịch, hoàn tiền");
        log.debug("Roles seeded");
    }

    private void seed(String name, String description) {
        if (roleRepository.findByName(name).isEmpty()) {
            roleRepository.save(Role.builder().name(name).description(description).build());
            log.info("Created role: {}", name);
        }
    }
}
