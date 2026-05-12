package com.flourishtravel.domain.tour.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.domain.tour.dto.CategoryRequest;
import com.flourishtravel.domain.tour.entity.Category;
import com.flourishtravel.domain.tour.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /** Public: chỉ trả các danh mục đang hoạt động. */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Category>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.listActive()));
    }

    /** Admin: danh sách danh mục đã lưu trữ. */
    @GetMapping("/archived")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Category>>> listArchived() {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.listArchived()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Category>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.getActiveById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Category>> create(@Valid @RequestBody CategoryRequest request) {
        Category created = categoryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo danh mục thành công", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Category>> update(@PathVariable UUID id,
                                                        @Valid @RequestBody CategoryRequest request) {
        Category updated = categoryService.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật danh mục thành công", updated));
    }

    /** Soft delete: đánh dấu deletedAt, không xoá vật lý. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        categoryService.softDelete(id);
        return ResponseEntity.ok(ApiResponse.ok("Đã lưu trữ danh mục", null));
    }

    /** Khôi phục danh mục đã lưu trữ. */
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Category>> restore(@PathVariable UUID id) {
        Category restored = categoryService.restore(id);
        return ResponseEntity.ok(ApiResponse.ok("Đã khôi phục danh mục", restored));
    }
}
