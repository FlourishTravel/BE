package com.flourishtravel.config;

import com.flourishtravel.domain.booking.entity.Promotion;
import com.flourishtravel.domain.booking.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PromotionSeeder {

    private final PromotionRepository promotionRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Order(7)
    @Transactional
    public void seed() {
        if (promotionRepository.count() > 0) {
            log.debug("Promotions already exist, skip seed");
            return;
        }
        Instant from = Instant.now();
        Instant to = Instant.now().plus(90, ChronoUnit.DAYS);
        List<Promotion> list = List.of(
                Promotion.builder()
                        .code("WELCOME50")
                        .name("Giảm 50k cho đơn đầu")
                        .discountType("fixed")
                        .discountValue(new BigDecimal("50000"))
                        .minOrderAmount(new BigDecimal("2000000"))
                        .validFrom(from).validTo(to).usageLimit(1000).usedCount(0).isActive(true)
                        .build(),
                Promotion.builder()
                        .code("SUMMER5")
                        .name("Giảm 5% tour hè")
                        .discountType("percent")
                        .discountValue(new BigDecimal("5"))
                        .minOrderAmount(new BigDecimal("3000000"))
                        .maxDiscountAmount(new BigDecimal("500000"))
                        .validFrom(from).validTo(to).usageLimit(500).usedCount(0).isActive(true)
                        .build(),
                Promotion.builder()
                        .code("CHAT5")
                        .name("Đặt qua chat giảm 5%")
                        .discountType("percent")
                        .discountValue(new BigDecimal("5"))
                        .minOrderAmount(BigDecimal.ZERO)
                        .maxDiscountAmount(new BigDecimal("300000"))
                        .validFrom(from).validTo(to).usageLimit(null).usedCount(0).isActive(true)
                        .build()
        );
        promotionRepository.saveAll(list);
        log.info("Seeded {} promotions", list.size());
    }
}
