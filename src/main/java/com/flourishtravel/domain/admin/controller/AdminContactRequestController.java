package com.flourishtravel.domain.admin.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.common.exception.ResourceNotFoundException;
import com.flourishtravel.domain.contact.entity.ContactRequest;
import com.flourishtravel.domain.contact.repository.ContactRequestRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminContactRequestController {

    private final ContactRequestRepository contactRequestRepository;

    @GetMapping("/contact-requests")
    public ResponseEntity<ApiResponse<Page<ContactRequest>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pr = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ContactRequest> result = status != null && !status.isBlank()
                ? contactRequestRepository.findByStatusOrderByCreatedAtDesc(status, pr)
                : contactRequestRepository.findAllByOrderByCreatedAtDesc(pr);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/contact-requests/{id}")
    public ResponseEntity<ApiResponse<ContactRequest>> getById(@PathVariable UUID id) {
        ContactRequest cr = contactRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ContactRequest", id));
        return ResponseEntity.ok(ApiResponse.ok(cr));
    }

    @PatchMapping("/contact-requests/{id}")
    public ResponseEntity<ApiResponse<ContactRequest>> update(
            @PathVariable UUID id,
            @RequestBody UpdateContactRequestRequest body) {
        ContactRequest cr = contactRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ContactRequest", id));
        if (body.getStatus() != null) {
            cr.setStatus(body.getStatus());
        }
        if (body.getNote() != null) {
            cr.setNote(body.getNote());
        }
        cr = contactRequestRepository.save(cr);
        return ResponseEntity.ok(ApiResponse.ok("Đã cập nhật", cr));
    }

    @Data
    public static class UpdateContactRequestRequest {
        private String status;
        private String note;
    }
}
