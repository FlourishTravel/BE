package com.flourishtravel.domain.tour.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.common.exception.ResourceNotFoundException;
import com.flourishtravel.domain.tour.entity.Category;
import com.flourishtravel.domain.tour.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository categoryRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Category>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(categoryRepository.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Category>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id))));
    }
}
