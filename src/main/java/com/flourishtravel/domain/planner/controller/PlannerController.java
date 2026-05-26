package com.flourishtravel.domain.planner.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.domain.planner.dto.*;
import com.flourishtravel.domain.planner.service.PlannerService;
import com.flourishtravel.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/planner")
@RequiredArgsConstructor
public class PlannerController {

    private final PlannerService plannerService;

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<PlannerGenerateResponse>> generate(
            @RequestBody PlannerGenerateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(plannerService.generate(request)));
    }

    @GetMapping("/suggestions")
    public ResponseEntity<ApiResponse<PlannerSuggestionDto>> suggestions(
            @RequestParam String city,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.ok(plannerService.getSuggestion(city, date)));
    }

    @PostMapping("/calculate-budget")
    public ResponseEntity<ApiResponse<PlannerBudgetDto>> calculateBudget(
            @RequestBody PlannerBudgetRequest body) {
        return ResponseEntity.ok(ApiResponse.ok(
                plannerService.calculateBudgetFromDays(body.getRequest(), body.getDays())));
    }

    @PostMapping("/save")
    public ResponseEntity<ApiResponse<SavedTripPlanDto>> save(
            @RequestBody PlannerSaveRequest body,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(plannerService.save(body, principal)));
    }

    @GetMapping("/saved")
    public ResponseEntity<ApiResponse<List<SavedTripPlanDto>>> saved(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(plannerService.listSaved(principal)));
    }

    @GetMapping("/export/pdf/{planId}")
    public ResponseEntity<ApiResponse<Map<String, String>>> exportPdf(
            @PathVariable UUID planId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(plannerService.exportPdf(planId, principal)));
    }

    @PostMapping("/sync-calendar/{planId}")
    public ResponseEntity<ApiResponse<Map<String, String>>> syncCalendar(
            @PathVariable UUID planId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(plannerService.syncCalendar(planId, principal)));
    }
}
