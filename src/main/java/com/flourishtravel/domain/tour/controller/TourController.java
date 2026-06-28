package com.flourishtravel.domain.tour.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.domain.tour.dto.AvailabilityCheckDto;
import com.flourishtravel.domain.tour.dto.GeocodeResultDto;
import com.flourishtravel.domain.tour.dto.ItineraryRequest;
import com.flourishtravel.domain.tour.dto.LocationRequest;
import com.flourishtravel.domain.tour.dto.TourDetailDto;
import com.flourishtravel.domain.tour.dto.TourRequest;
import com.flourishtravel.domain.tour.dto.TourSummaryDto;
import com.flourishtravel.domain.tour.entity.Tour;
import com.flourishtravel.domain.tour.service.GeocodeService;
import com.flourishtravel.domain.tour.service.TourService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final GeocodeService geocodeService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<TourSummaryDto>>> list(
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String segment,
            @RequestParam(required = false, defaultValue = "date_asc") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Page<TourSummaryDto> tours = startDate != null
                ? tourService.publicCatalog(destination, minPrice, maxPrice, startDate, categoryId,
                        PageRequest.of(page, size))
                : tourService.publicCatalogBrowse(destination, minPrice, maxPrice, categoryId, segment,
                        PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok(tours));
    }

    /**
     * Admin list: trả toàn bộ tour (không lọc theo availability), kèm category, ảnh đại diện,
     * session sớm nhất và status suy luận.
     */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<TourSummaryDto>>> adminList(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<TourSummaryDto> result = tourService.adminList(q, status, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /** Tra cứu tọa độ địa điểm qua VietMap (admin itinerary builder). */
    @GetMapping("/admin/geocode")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<GeocodeResultDto>> geocodeActivity(
            @RequestParam(required = false) String locationName,
            @RequestParam(required = false) String locationAddress,
            @RequestParam(required = false) String destinationCity) {
        return ResponseEntity.ok(ApiResponse.ok(
                geocodeService.resolveActivityCoordinates(locationName, locationAddress, destinationCity)));
    }

    /** Chi tiết tour cho admin (full quan hệ: ảnh, video, session, lịch trình, địa điểm). */
    @GetMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TourDetailDto>> adminDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(tourService.getAdminDetail(id)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TourDetailDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(tourService.getPublicDetail(id)));
    }

    @GetMapping("/by-slug/{slug}")
    public ResponseEntity<ApiResponse<TourDetailDto>> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.ok(tourService.getPublicDetailBySlug(slug)));
    }

    /** Tour tương tự / Có thể bạn cũng thích (cùng danh mục hoặc mới nhất). */
    @GetMapping("/{id}/similar")
    public ResponseEntity<ApiResponse<List<TourSummaryDto>>> getSimilar(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "4") int limit) {
        List<TourSummaryDto> list = tourService.getSimilarSummaries(id, Math.min(limit, 20));
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

    // ---------- Admin write APIs ----------

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Tour>> create(@Valid @RequestBody TourRequest request) {
        Tour created = tourService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo tour thành công", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Tour>> update(@PathVariable UUID id,
                                                    @Valid @RequestBody TourRequest request) {
        Tour updated = tourService.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật tour thành công", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        tourService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Đã xoá tour", null));
    }

    // ---------- Itinerary Builder APIs ----------

    /** Lấy chi tiết lịch trình (kèm activities) cho Itinerary Builder. */
    @GetMapping("/admin/{id}/itinerary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<TourDetailDto.ItineraryRef>>> getItinerary(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(tourService.getItinerary(id)));
    }

    /** Lưu toàn bộ lịch trình (bulk replace). */
    @PutMapping("/admin/{id}/itinerary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<TourDetailDto.ItineraryRef>>> saveItinerary(
            @PathVariable UUID id,
            @Valid @RequestBody List<@Valid ItineraryRequest> days) {
        return ResponseEntity.ok(ApiResponse.ok("Đã lưu lịch trình", tourService.saveItinerary(id, days)));
    }

    /** Lấy danh sách địa điểm tour (theo ngày lịch trình). */
    @GetMapping("/admin/{id}/locations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<TourDetailDto.LocationRef>>> getLocations(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(tourService.getLocations(id)));
    }

    /** Lưu toàn bộ địa điểm tour (bulk replace). */
    @PutMapping("/admin/{id}/locations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<TourDetailDto.LocationRef>>> saveLocations(
            @PathVariable UUID id,
            @Valid @RequestBody List<@Valid LocationRequest> locations) {
        return ResponseEntity.ok(ApiResponse.ok("Đã lưu địa điểm", tourService.saveLocations(id, locations)));
    }
}
