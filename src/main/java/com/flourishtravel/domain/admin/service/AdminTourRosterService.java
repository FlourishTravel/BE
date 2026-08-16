package com.flourishtravel.domain.admin.service;

import com.flourishtravel.common.exception.ResourceNotFoundException;
import com.flourishtravel.domain.admin.dto.AdminWaitlistEntryDto;
import com.flourishtravel.domain.booking.entity.SessionWaitlist;
import com.flourishtravel.domain.booking.repository.SessionWaitlistRepository;
import com.flourishtravel.domain.guide.dto.GuideSessionGuestsDto;
import com.flourishtravel.domain.guide.service.GuideService;
import com.flourishtravel.domain.tour.entity.Tour;
import com.flourishtravel.domain.tour.entity.TourSession;
import com.flourishtravel.domain.tour.repository.TourRepository;
import com.flourishtravel.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminTourRosterService {

    private final TourRepository tourRepository;
    private final SessionWaitlistRepository waitlistRepository;
    private final GuideService guideService;

    @Transactional
    public GuideSessionGuestsDto sessionGuests(UUID sessionId) {
        return guideService.getSessionGuestsForAdmin(sessionId);
    }

    @Transactional(readOnly = true)
    public List<AdminWaitlistEntryDto> waitlistForTour(UUID tourId) {
        tourRepository.findById(tourId).orElseThrow(() -> new ResourceNotFoundException("Tour", tourId));
        List<SessionWaitlist> rows = new ArrayList<>();
        rows.addAll(waitlistRepository.findTourLevelByTourId(tourId));
        rows.addAll(waitlistRepository.findSessionLevelByTourId(tourId));
        rows.sort(Comparator.comparing(SessionWaitlist::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return rows.stream().map(this::toDto).toList();
    }

    private AdminWaitlistEntryDto toDto(SessionWaitlist w) {
        User u = w.getUser();
        TourSession session = w.getSession();
        Tour tour = w.getTour();
        if (tour == null && session != null) {
            tour = session.getTour();
        }
        boolean sessionScoped = session != null;
        return AdminWaitlistEntryDto.builder()
                .id(w.getId())
                .userId(u != null ? u.getId() : null)
                .fullName(u != null ? u.getFullName() : null)
                .email(u != null ? u.getEmail() : null)
                .phone(u != null ? u.getPhone() : null)
                .tourId(tour != null ? tour.getId() : null)
                .sessionId(session != null ? session.getId() : null)
                .sessionStartDate(session != null ? session.getStartDate() : null)
                .sessionEndDate(session != null ? session.getEndDate() : null)
                .scope(sessionScoped ? "session" : "tour")
                .status(w.getStatus())
                .createdAt(w.getCreatedAt())
                .build();
    }
}
