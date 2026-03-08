package com.flourishtravel.domain.admin.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.domain.admin.dto.AdminStatsResponse;
import com.flourishtravel.domain.admin.service.AdminStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminStatsController {

    private final AdminStatsService adminStatsService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getStats() {
        AdminStatsResponse stats = adminStatsService.getStats();
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }
}
