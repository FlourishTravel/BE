package com.flourishtravel.domain.user.service;

import com.flourishtravel.common.exception.ResourceNotFoundException;
import com.flourishtravel.domain.tour.entity.Tour;
import com.flourishtravel.domain.tour.entity.TourImage;
import com.flourishtravel.domain.tour.entity.TourSession;
import com.flourishtravel.domain.tour.repository.TourSessionRepository;
import com.flourishtravel.domain.user.dto.PublicGuideSummaryDto;
import com.flourishtravel.domain.user.entity.User;
import com.flourishtravel.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
                .map(user -> toDto(user, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public PublicGuideSummaryDto getGuide(UUID id) {
        User guide = userRepository.findByIdAndRole_NameAndIsActiveTrue(id, GUIDE_ROLE)
                .orElseThrow(() -> new ResourceNotFoundException("Guide", id));
        return toDto(guide, true);
    }

    private PublicGuideSummaryDto toDto(User user, boolean includeTours) {
        PublicGuideSummaryDto.PublicGuideSummaryDtoBuilder builder = PublicGuideSummaryDto.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .jobTitle(user.getJobTitle() != null && !user.getJobTitle().isBlank()
                        ? user.getJobTitle()
                        : "Hướng dẫn viên")
                .department(user.getDepartment())
                .languages(List.of())
                .toursCompleted(tourSessionRepository.countByTourGuide_Id(user.getId()));
        if (includeTours) {
            builder.tours(assignedTours(user.getId()));
        }
        return builder.build();
    }

    private List<PublicGuideSummaryDto.AssignedTourRef> assignedTours(UUID guideId) {
        List<TourSession> sessions = tourSessionRepository.findAssignedWithTour(guideId);
        Map<UUID, PublicGuideSummaryDto.AssignedTourRef> byTour = new LinkedHashMap<>();
        for (TourSession session : sessions) {
            Tour tour = session.getTour();
            if (tour == null || tour.getId() == null) {
                continue;
            }
            LocalDate start = session.getStartDate();
            PublicGuideSummaryDto.AssignedTourRef existing = byTour.get(tour.getId());
            if (existing == null) {
                byTour.put(tour.getId(), PublicGuideSummaryDto.AssignedTourRef.builder()
                        .id(tour.getId())
                        .title(tour.getTitle())
                        .durationDays(tour.getDurationDays())
                        .durationNights(tour.getDurationNights())
                        .basePrice(tour.getBasePrice())
                        .thumbnailUrl(firstImageUrl(tour))
                        .nextStartDate(start)
                        .build());
                continue;
            }
            if (start != null && (existing.getNextStartDate() == null || start.isBefore(existing.getNextStartDate()))) {
                existing.setNextStartDate(start);
            }
        }
        List<PublicGuideSummaryDto.AssignedTourRef> tours = new ArrayList<>(byTour.values());
        tours.sort(Comparator.comparing(
                PublicGuideSummaryDto.AssignedTourRef::getNextStartDate,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return tours;
    }

    private static String firstImageUrl(Tour tour) {
        if (tour.getImages() == null || tour.getImages().isEmpty()) {
            return null;
        }
        return tour.getImages().stream()
                .sorted(Comparator.comparing(TourImage::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(TourImage::getImageUrl)
                .filter(url -> url != null && !url.isBlank())
                .findFirst()
                .orElse(null);
    }
}
