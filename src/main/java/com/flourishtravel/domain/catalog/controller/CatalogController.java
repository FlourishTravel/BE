package com.flourishtravel.domain.catalog.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.domain.catalog.dto.*;
import com.flourishtravel.domain.catalog.service.CatalogService;
import com.flourishtravel.domain.tour.dto.TourDetailDto;
import com.flourishtravel.domain.tour.service.TourService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;
    private final TourService tourService;

    @GetMapping
    public ResponseEntity<ApiResponse<CatalogPageDto>> search(
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) Integer guests,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                catalogService.search(destination, startDate, guests, type, page, size)));
    }

    @GetMapping("/tickets")
    public ResponseEntity<ApiResponse<List<TicketCardDto>>> tickets(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String destination) {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.listTickets(category, destination)));
    }

    @GetMapping("/tickets/{slug}")
    public ResponseEntity<ApiResponse<TicketCardDto>> ticket(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.getTicketBySlug(slug)));
    }

    @PostMapping("/flora-recommend")
    public ResponseEntity<ApiResponse<FloraTourRecommendDto>> floraRecommend(
            @RequestBody FloraTourRecommendRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.floraRecommend(request)));
    }

    @GetMapping("/tours/{id}/detail")
    public ResponseEntity<ApiResponse<TourDetailDto>> tourDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(
                catalogService.enrichTourDetail(tourService.getPublicDetail(id))));
    }
}
