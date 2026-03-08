package com.flourishtravel.domain.contact.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.domain.contact.entity.ContactRequest;
import com.flourishtravel.domain.contact.repository.ContactRequestRepository;
import com.flourishtravel.domain.tour.repository.TourRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/contact-requests")
@RequiredArgsConstructor
public class ContactRequestController {

    private final ContactRequestRepository contactRequestRepository;
    private final TourRepository tourRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<ContactRequest>> create(@Valid @RequestBody ContactRequestCreateDto dto) {
        ContactRequest entity = ContactRequest.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .message(dto.getMessage())
                .tour(dto.getTourId() != null ? tourRepository.findById(dto.getTourId()).orElse(null) : null)
                .status("new")
                .build();
        entity = contactRequestRepository.save(entity);
        return ResponseEntity.ok(ApiResponse.ok("Đã gửi thông tin, chúng tôi sẽ liên hệ sớm", entity));
    }

    @Data
    public static class ContactRequestCreateDto {
        @NotBlank(message = "Tên không được để trống")
        private String name;
        @NotBlank
        @Email
        private String email;
        private String phone;
        @NotBlank(message = "Nội dung không được để trống")
        private String message;
        private UUID tourId;
    }
}
