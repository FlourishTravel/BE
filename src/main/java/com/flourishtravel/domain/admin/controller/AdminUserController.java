package com.flourishtravel.domain.admin.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.common.exception.ResourceNotFoundException;
import com.flourishtravel.domain.user.entity.Role;
import com.flourishtravel.domain.user.entity.User;
import com.flourishtravel.domain.user.repository.RoleRepository;
import com.flourishtravel.domain.user.repository.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @PatchMapping("/users/{id}")
    public ResponseEntity<ApiResponse<User>> updateUser(
            @AuthenticationPrincipal com.flourishtravel.security.UserPrincipal principal,
            @PathVariable UUID id,
            @RequestBody AdminUserUpdateDto dto) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
        if (dto.getRoleId() != null) {
            Role role = roleRepository.findById(dto.getRoleId()).orElseThrow(() -> new ResourceNotFoundException("Role", dto.getRoleId()));
            user.setRole(role);
        }
        if (dto.getIsActive() != null) {
            user.setIsActive(dto.getIsActive());
        }
        user = userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.ok("Đã cập nhật user", user));
    }

    @Data
    public static class AdminUserUpdateDto {
        private UUID roleId;
        private Boolean isActive;
    }
}
