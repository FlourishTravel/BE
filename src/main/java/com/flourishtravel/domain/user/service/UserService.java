package com.flourishtravel.domain.user.service;

import com.flourishtravel.common.exception.ResourceNotFoundException;
import com.flourishtravel.domain.flora.dto.TravelPreferencesDto;
import com.flourishtravel.domain.flora.dto.UpdateTravelPreferencesRequest;
import com.flourishtravel.domain.flora.service.UserTravelPreferenceService;
import com.flourishtravel.domain.user.dto.UpdateProfileRequest;
import com.flourishtravel.domain.user.dto.UserProfileResponse;
import com.flourishtravel.domain.user.entity.User;
import com.flourishtravel.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserTravelPreferenceService travelPreferenceService;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return toProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName().trim());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().trim().isEmpty() ? null : request.getPhone().trim());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl().trim().isEmpty() ? null : request.getAvatarUrl().trim());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender().trim().isEmpty() ? null : request.getGender().trim());
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress().trim().isEmpty() ? null : request.getAddress().trim());
        }
        user = userRepository.save(user);
        return toProfileResponse(user);
    }

    @Transactional(readOnly = true)
    public TravelPreferencesDto getTravelPreferences(UUID userId) {
        userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return travelPreferenceService.getForUser(userId);
    }

    @Transactional
    public TravelPreferencesDto updateTravelPreferences(UUID userId, UpdateTravelPreferencesRequest request) {
        userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return travelPreferenceService.update(userId, request);
    }

    private UserProfileResponse toProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .gender(user.getGender())
                .address(user.getAddress())
                .role(user.getRole() != null ? user.getRole().getName() : null)
                .build();
    }
}
