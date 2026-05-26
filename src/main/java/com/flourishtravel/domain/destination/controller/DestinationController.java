package com.flourishtravel.domain.destination.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.domain.destination.dto.*;
import com.flourishtravel.domain.destination.service.DestinationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/destinations")
@RequiredArgsConstructor
public class DestinationController {

    private final DestinationService destinationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DestinationSummaryDto>>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(ApiResponse.ok(destinationService.list(type, q)));
    }

    @GetMapping("/festivals")
    public ResponseEntity<ApiResponse<List<ThaiFestivalDto>>> festivals() {
        return ResponseEntity.ok(ApiResponse.ok(destinationService.listFestivals()));
    }

    @GetMapping("/festivals/{festivalSlug}")
    public ResponseEntity<ApiResponse<ThaiFestivalDetailDto>> festival(
            @PathVariable String festivalSlug) {
        return ResponseEntity.ok(ApiResponse.ok(destinationService.getFestival(festivalSlug)));
    }

    @PostMapping("/flora-match")
    public ResponseEntity<ApiResponse<DestinationDetailDto.FloraMatchDto>> floraMatch(
            @RequestBody FloraMatchRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(destinationService.floraMatch(request)));
    }

    @GetMapping("/{slug}/map-stats")
    public ResponseEntity<ApiResponse<MapStatsDto>> mapStats(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.ok(destinationService.mapStats(slug)));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<DestinationDetailDto>> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.ok(destinationService.getBySlug(slug)));
    }
}
