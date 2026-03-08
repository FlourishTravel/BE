package com.flourishtravel.config;

import com.flourishtravel.domain.user.entity.Role;
import com.flourishtravel.domain.user.entity.User;
import com.flourishtravel.domain.user.repository.RoleRepository;
import com.flourishtravel.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserSeeder {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @EventListener(ApplicationReadyEvent.class)
    @Order(3)
    @Transactional
    public void seed() {
        if (userRepository.count() > 0) {
            log.debug("Users already exist, skip seed");
            return;
        }
        Role adminRole = roleRepository.findByName("ADMIN").orElseThrow(() -> new IllegalStateException("Role ADMIN not found. Run RoleSeeder first."));
        Role travelerRole = roleRepository.findByName("TRAVELER").orElseThrow(() -> new IllegalStateException("Role TRAVELER not found."));

        User admin = User.builder()
                .email("admin@flourishtravel.com")
                .passwordHash(passwordEncoder.encode("Admin@123"))
                .fullName("Admin FlourishTravel")
                .phone("0901234567")
                .role(adminRole)
                .isActive(true)
                .build();
        userRepository.save(admin);

        User traveler = User.builder()
                .email("traveler@example.com")
                .passwordHash(passwordEncoder.encode("Traveler@123"))
                .fullName("Nguyễn Văn Du lịch")
                .phone("0912345678")
                .role(travelerRole)
                .isActive(true)
                .build();
        userRepository.save(traveler);

        log.info("Seeded 2 users: admin@flourishtravel.com, traveler@example.com (pass: Admin@123, Traveler@123)");
    }
}
