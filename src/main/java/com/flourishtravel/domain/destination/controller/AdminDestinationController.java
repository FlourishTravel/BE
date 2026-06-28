package com.flourishtravel.domain.destination.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.domain.destination.dto.AdminDestinationRequest;
import com.flourishtravel.domain.destination.dto.DestinationSummaryDto;
import com.flourishtravel.domain.destination.service.AdminDestinationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/destinations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDestinationController {

    private final AdminDestinationService adminDestinationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DestinationSummaryDto>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(adminDestinationService.listAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DestinationSummaryDto>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(adminDestinationService.get(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DestinationSummaryDto>> create(
            @Valid @RequestBody AdminDestinationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Đã tạo điểm đến", adminDestinationService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DestinationSummaryDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody AdminDestinationRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Đã cập nhật điểm đến", adminDestinationService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        adminDestinationService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Đã xóa điểm đến", null));
    }
}
