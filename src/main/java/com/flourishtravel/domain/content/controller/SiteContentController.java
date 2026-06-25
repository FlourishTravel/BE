package com.flourishtravel.domain.content.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.domain.content.dto.SiteContentDto;
import com.flourishtravel.domain.content.service.SiteContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/content")
@RequiredArgsConstructor
public class SiteContentController {

    private final SiteContentService siteContentService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SiteContentDto>>> list(
            @RequestParam(required = false) String type) {
        return ResponseEntity.ok(ApiResponse.ok(siteContentService.listPublic(type)));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<SiteContentDto>> get(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.ok(siteContentService.getPublicBySlug(slug)));
    }
}
