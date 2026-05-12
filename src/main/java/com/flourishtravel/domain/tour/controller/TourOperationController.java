package com.flourishtravel.domain.tour.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.domain.tour.dto.AssignGuideRequest;
import com.flourishtravel.domain.tour.dto.GuideAvailabilityDto;
import com.flourishtravel.domain.tour.dto.SessionStatusRequest;
import com.flourishtravel.domain.tour.dto.TourOperationDto;
import com.flourishtravel.domain.tour.service.TourOperationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * REST endpoints cho trang Điều hành tour (Tour Operations / Dispatch).
 *
 * Tất cả endpoints đều yêu cầu role ADMIN.
 *
 *  - GET    /tour-operations/sessions?from=YYYY-MM-DD&to=YYYY-MM-DD&q=
 *  - GET    /tour-operations/guides?date=YYYY-MM-DD&excludeSessionId=
 *  - PUT    /tour-operations/sessions/{id}/guide          {guideId, notify, note}
 *  - DELETE /tour-operations/sessions/{id}/guide
 *  - PUT    /tour-operations/sessions/{id}/status        {status, note}
 */
@RestController
@RequestMapping("/tour-operations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class TourOperationController {

    private final TourOperationService operationService;

    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<List<TourOperationDto>>> listSessions(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(ApiResponse.ok(operationService.listSessions(from, to, q)));
    }

    @GetMapping("/guides")
    public ResponseEntity<ApiResponse<List<GuideAvailabilityDto>>> listGuides(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) UUID excludeSessionId) {
        return ResponseEntity.ok(ApiResponse.ok(operationService.listGuides(date, excludeSessionId)));
    }

    @PutMapping("/sessions/{id}/guide")
    public ResponseEntity<ApiResponse<TourOperationDto>> assignGuide(
            @PathVariable UUID id,
            @Valid @RequestBody AssignGuideRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Đã phân công HDV",
                operationService.assignGuide(id, request)));
    }

    @DeleteMapping("/sessions/{id}/guide")
    public ResponseEntity<ApiResponse<TourOperationDto>> unassignGuide(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Đã huỷ phân công",
                operationService.unassignGuide(id)));
    }

    @PutMapping("/sessions/{id}/status")
    public ResponseEntity<ApiResponse<TourOperationDto>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody SessionStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Đã cập nhật trạng thái session",
                operationService.updateStatus(id, request)));
    }
}
