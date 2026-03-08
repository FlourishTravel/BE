package com.flourishtravel.config;

import com.flourishtravel.domain.tour.entity.Category;
import com.flourishtravel.domain.tour.repository.CategoryRepository;
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
public class CategorySeeder {

    private final CategoryRepository categoryRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Order(2)
    @Transactional
    public void seed() {
        if (categoryRepository.count() > 0) {
            log.debug("Categories already exist, skip seed");
            return;
        }
        List<Category> list = List.of(
                Category.builder().name("Tour biển").slug("tour-bien").description("Tour nghỉ dưỡng biển").sortOrder(1).build(),
                Category.builder().name("Khám phá").slug("kham-pha").description("Tour khám phá văn hóa, thiên nhiên").sortOrder(2).build()
        );
        categoryRepository.saveAll(list);
        log.info("Seeded {} categories", list.size());
    }
}
