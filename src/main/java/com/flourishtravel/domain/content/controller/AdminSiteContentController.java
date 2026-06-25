package com.flourishtravel.domain.content.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.domain.content.dto.CreateSiteContentRequest;
import com.flourishtravel.domain.content.dto.SiteContentDto;
import com.flourishtravel.domain.content.dto.UpdateSiteContentRequest;
import com.flourishtravel.domain.content.service.SiteContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/content")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSiteContentController {

    private final SiteContentService siteContentService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SiteContentDto>>> list(
            @RequestParam(required = false) String type) {
        return ResponseEntity.ok(ApiResponse.ok(siteContentService.listAdmin(type)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SiteContentDto>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(siteContentService.getAdmin(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SiteContentDto>> create(@Valid @RequestBody CreateSiteContentRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Đã tạo nội dung", siteContentService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SiteContentDto>> update(
            @PathVariable UUID id,
            @RequestBody UpdateSiteContentRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Đã cập nhật nội dung", siteContentService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        siteContentService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Đã xóa nội dung", null));
    }
}
