package com.flourishtravel.domain.user.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.domain.user.dto.PublicGuideSummaryDto;
import com.flourishtravel.domain.user.service.PublicGuideService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PublicGuideController {

    private final PublicGuideService publicGuideService;

    @GetMapping("/guides")
    public ResponseEntity<ApiResponse<List<PublicGuideSummaryDto>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(publicGuideService.listActiveGuides()));
    }

    @GetMapping("/guides/{id}")
    public ResponseEntity<ApiResponse<PublicGuideSummaryDto>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(publicGuideService.getGuide(id)));
    }
}
