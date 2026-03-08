package com.flourishtravel.domain.admin.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.common.exception.ResourceNotFoundException;
import com.flourishtravel.domain.tour.entity.Category;
import com.flourishtravel.domain.tour.entity.Tour;
import com.flourishtravel.domain.tour.repository.CategoryRepository;
import com.flourishtravel.domain.tour.repository.TourRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminTourController {

    private final TourRepository tourRepository;
    private final CategoryRepository categoryRepository;

    @PostMapping("/tours")
    public ResponseEntity<ApiResponse<Tour>> create(@RequestBody AdminTourDto dto) {
        Category category = dto.getCategoryId() != null
                ? categoryRepository.findById(dto.getCategoryId()).orElse(null)
                : null;
        String slug = dto.getSlug() != null && !dto.getSlug().isBlank()
                ? dto.getSlug().trim().toLowerCase().replace(" ", "-")
                : (dto.getTitle() != null ? dto.getTitle().trim().toLowerCase().replace(" ", "-") : "tour-" + UUID.randomUUID().toString().substring(0, 8));
        if (tourRepository.findBySlug(slug).isPresent()) {
            slug = slug + "-" + System.currentTimeMillis();
        }
        Tour tour = Tour.builder()
                .title(dto.getTitle())
                .slug(slug)
                .description(dto.getDescription())
                .basePrice(dto.getBasePrice())
                .durationDays(dto.getDurationDays())
                .durationNights(dto.getDurationNights())
                .category(category)
                .build();
        tour = tourRepository.save(tour);
        return ResponseEntity.ok(ApiResponse.ok("Đã tạo tour", tour));
    }

    @PutMapping("/tours/{id}")
    public ResponseEntity<ApiResponse<Tour>> update(@PathVariable UUID id, @RequestBody AdminTourDto dto) {
        Tour tour = tourRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Tour", id));
        if (dto.getTitle() != null) tour.setTitle(dto.getTitle());
        if (dto.getDescription() != null) tour.setDescription(dto.getDescription());
        if (dto.getBasePrice() != null) tour.setBasePrice(dto.getBasePrice());
        if (dto.getDurationDays() != null) tour.setDurationDays(dto.getDurationDays());
        if (dto.getDurationNights() != null) tour.setDurationNights(dto.getDurationNights());
        if (dto.getCategoryId() != null) {
            tour.setCategory(categoryRepository.findById(dto.getCategoryId()).orElse(null));
        }
        tour = tourRepository.save(tour);
        return ResponseEntity.ok(ApiResponse.ok("Đã cập nhật tour", tour));
    }

    @DeleteMapping("/tours/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        Tour tour = tourRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Tour", id));
        tourRepository.delete(tour);
        return ResponseEntity.ok(ApiResponse.ok("Đã xóa tour", null));
    }

    @Data
    public static class AdminTourDto {
        private String title;
        private String slug;
        private String description;
        private BigDecimal basePrice;
        private Integer durationDays;
        private Integer durationNights;
        private UUID categoryId;
    }
}
