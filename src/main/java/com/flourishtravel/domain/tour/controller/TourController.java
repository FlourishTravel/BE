package com.flourishtravel.domain.tour.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.domain.tour.entity.Tour;
import com.flourishtravel.domain.tour.service.TourService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tours")
@RequiredArgsConstructor
public class TourController {

    private final TourService tourService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<Tour>>> list(
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false, defaultValue = "date_asc") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Sort sortBy = switch (sort != null ? sort : "") {
            case "price_asc" -> Sort.by("basePrice").ascending();
            case "price_desc" -> Sort.by("basePrice").descending();
            case "date_asc" -> Sort.by("createdAt").ascending();
            case "popular" -> Sort.by("createdAt").descending();
            default -> Sort.by("createdAt").ascending();
        };
        Page<Tour> tours = tourService.search(destination, minPrice, maxPrice, startDate, categoryId,
                PageRequest.of(page, size, sortBy));
        return ResponseEntity.ok(ApiResponse.ok(tours));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Tour>> getById(@PathVariable UUID id) {
        Tour tour = tourService.getById(id);
        return ResponseEntity.ok(ApiResponse.ok(tour));
    }

    @GetMapping("/by-slug/{slug}")
    public ResponseEntity<ApiResponse<Tour>> getBySlug(@PathVariable String slug) {
        Tour tour = tourService.getBySlug(slug);
        return ResponseEntity.ok(ApiResponse.ok(tour));
    }

    /** Tour tương tự / Có thể bạn cũng thích (cùng danh mục hoặc mới nhất). */
    @GetMapping("/{id}/similar")
    public ResponseEntity<ApiResponse<List<Tour>>> getSimilar(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "4") int limit) {
        List<Tour> list = tourService.getSimilarTours(id, Math.min(limit, 20));
        return ResponseEntity.ok(ApiResponse.ok(list));
    }
}
