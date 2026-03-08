package com.flourishtravel.domain.admin.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.domain.audit.entity.AuditLog;
import com.flourishtravel.domain.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminAuditLogController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> list(
            @RequestParam(required = false) String entity_type,
            @RequestParam(required = false) UUID user_id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pr = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AuditLog> result;
        if (entity_type != null && !entity_type.isBlank()) {
            result = auditLogRepository.findByEntityTypeOrderByCreatedAtDesc(entity_type, pr);
        } else if (user_id != null) {
            result = auditLogRepository.findByUser_IdOrderByCreatedAtDesc(user_id, pr);
        } else if (from != null && to != null) {
            result = auditLogRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(from, to, pr);
        } else {
            result = auditLogRepository.findAll(pr);
        }
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
