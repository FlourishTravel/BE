package com.flourishtravel.domain.tour.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.domain.tour.dto.AvailabilityCheckDto;
import com.flourishtravel.domain.tour.entity.Tour;
import com.flourishtravel.domain.tour.service.TourService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
        // Native query đã có ORDER BY t.created_at; không truyền Sort để tránh Spring nối "t.createdAt" (sai tên cột PostgreSQL)
        Page<Tour> tours = tourService.search(destination, minPrice, maxPrice, startDate, categoryId,
                PageRequest.of(page, size));
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

    /** Kiểm tra còn chỗ tour theo địa điểm (cho chatbot / đặt vé). */
    @GetMapping("/availability/check")
    public ResponseEntity<ApiResponse<AvailabilityCheckDto>> checkAvailability(
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate) {
        return tourService.checkAvailability(destination, startDate)
                .map(dto -> ResponseEntity.ok(ApiResponse.ok(dto)))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.ok(null)));
    }
}
