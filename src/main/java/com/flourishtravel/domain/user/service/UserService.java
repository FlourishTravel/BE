package com.flourishtravel.domain.user.service;

import com.flourishtravel.common.exception.BadRequestException;
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
            String avatar = request.getAvatarUrl().trim();
            if (avatar.isEmpty()) {
                user.setAvatarUrl(null);
            } else if (avatar.regionMatches(true, 0, "data:", 0, 5)) {
                throw new BadRequestException(
                        "Ảnh đại diện quá lớn hoặc sai định dạng. Hãy tải ảnh qua API /upload rồi lưu URL.");
            } else if (avatar.length() > 500) {
                throw new BadRequestException("avatarUrl tối đa 500 ký tự");
            } else {
                user.setAvatarUrl(avatar);
            }
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender().trim().isEmpty() ? null : request.getGender().trim());
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress().trim().isEmpty() ? null : request.getAddress().trim());
        }
        if (isTourGuide(user)) {
            applyGuidePublicFields(user, request);
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
                .jobTitle(user.getJobTitle())
                .guideShortBio(user.getGuideShortBio())
                .guideBio(user.getGuideBio())
                .guideLanguages(com.flourishtravel.domain.user.CsvLists.split(user.getGuideLanguages()))
                .guideSpecialties(com.flourishtravel.domain.user.CsvLists.split(user.getGuideSpecialties()))
                .guideCoverUrl(user.getGuideCoverUrl())
                .guideExperienceYears(user.getGuideExperienceYears())
                .guideBaseLocation(user.getGuideBaseLocation())
                .guideBadges(com.flourishtravel.domain.user.CsvLists.split(user.getGuideBadges()))
                .guideVerified(Boolean.TRUE.equals(user.getGuideVerified()))
                .guidePublicApproved(Boolean.TRUE.equals(user.getGuidePublicApproved()))
                .guidePendingReview(Boolean.TRUE.equals(user.getGuidePendingReview()))
                .build();
    }

    private static boolean isTourGuide(User user) {
        return user.getRole() != null && "TOUR_GUIDE".equalsIgnoreCase(user.getRole().getName());
    }

    private void applyGuidePublicFields(User user, UpdateProfileRequest request) {
        boolean touched = false;
        if (request.getGuideShortBio() != null) {
            user.setGuideShortBio(blankToNull(request.getGuideShortBio()));
            touched = true;
        }
        if (request.getGuideBio() != null) {
            user.setGuideBio(blankToNull(request.getGuideBio()));
            touched = true;
        }
        if (request.getGuideLanguages() != null) {
            user.setGuideLanguages(com.flourishtravel.domain.user.CsvLists.join(request.getGuideLanguages()));
            touched = true;
        }
        if (request.getGuideSpecialties() != null) {
            user.setGuideSpecialties(com.flourishtravel.domain.user.CsvLists.join(request.getGuideSpecialties()));
            touched = true;
        }
        if (request.getGuideCoverUrl() != null) {
            String cover = request.getGuideCoverUrl().trim();
            if (cover.isEmpty()) {
                user.setGuideCoverUrl(null);
            } else if (cover.regionMatches(true, 0, "data:", 0, 5)) {
                throw new BadRequestException("Ảnh bìa quá lớn. Hãy tải ảnh qua API /upload rồi lưu URL.");
            } else if (cover.length() > 500) {
                throw new BadRequestException("guideCoverUrl tối đa 500 ký tự");
            } else {
                user.setGuideCoverUrl(cover);
            }
            touched = true;
        }
        if (request.getGuideExperienceYears() != null) {
            int years = Math.max(0, Math.min(request.getGuideExperienceYears(), 50));
            user.setGuideExperienceYears(years);
            touched = true;
        }
        if (request.getGuideBaseLocation() != null) {
            user.setGuideBaseLocation(blankToNull(request.getGuideBaseLocation()));
            touched = true;
        }
        if (request.getAvatarUrl() != null) {
            touched = true;
        }
        if (touched) {
            user.setGuidePendingReview(true);
        }
    }

    private static String blankToNull(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        return value.trim();
    }
}
