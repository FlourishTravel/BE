package com.flourishtravel.domain.catalog.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.domain.catalog.dto.AdminTravelTicketDto;
import com.flourishtravel.domain.catalog.dto.AdminTravelTicketRequest;
import com.flourishtravel.domain.catalog.service.AdminCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/catalog/tickets")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCatalogController {

    private final AdminCatalogService adminCatalogService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminTravelTicketDto>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(adminCatalogService.listAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminTravelTicketDto>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(adminCatalogService.get(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AdminTravelTicketDto>> create(@RequestBody AdminTravelTicketRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Đã tạo ticket", adminCatalogService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminTravelTicketDto>> update(
            @PathVariable UUID id,
            @RequestBody AdminTravelTicketRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Đã cập nhật ticket", adminCatalogService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminTravelTicketDto>> delete(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Đã ẩn ticket", adminCatalogService.softDelete(id)));
    }
}
