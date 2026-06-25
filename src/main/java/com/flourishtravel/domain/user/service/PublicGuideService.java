package com.flourishtravel.domain.user.service;

import com.flourishtravel.common.exception.ResourceNotFoundException;
import com.flourishtravel.domain.tour.repository.TourSessionRepository;
import com.flourishtravel.domain.user.dto.PublicGuideSummaryDto;
import com.flourishtravel.domain.user.entity.User;
import com.flourishtravel.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PublicGuideService {

    private static final String GUIDE_ROLE = "TOUR_GUIDE";

    private final UserRepository userRepository;
    private final TourSessionRepository tourSessionRepository;

    @Transactional(readOnly = true)
    public List<PublicGuideSummaryDto> listActiveGuides() {
        return userRepository.findActiveByRoleName(GUIDE_ROLE).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PublicGuideSummaryDto getGuide(UUID id) {
        User guide = userRepository.findByIdAndRole_NameAndIsActiveTrue(id, GUIDE_ROLE)
                .orElseThrow(() -> new ResourceNotFoundException("Guide", id));
        return toDto(guide);
    }

    private PublicGuideSummaryDto toDto(User user) {
        return PublicGuideSummaryDto.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .jobTitle(user.getJobTitle())
                .department(user.getDepartment())
                .languages(List.of())
                .rating(new BigDecimal("4.8"))
                .toursCompleted(tourSessionRepository.countByTourGuide_Id(user.getId()))
                .build();
    }
}
