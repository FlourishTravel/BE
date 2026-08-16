package com.flourishtravel.domain.admin.service;

import com.flourishtravel.domain.booking.entity.SessionWaitlist;
import com.flourishtravel.domain.booking.repository.SessionWaitlistRepository;
import com.flourishtravel.domain.guide.service.GuideService;
import com.flourishtravel.domain.tour.entity.Tour;
import com.flourishtravel.domain.tour.entity.TourSession;
import com.flourishtravel.domain.tour.repository.TourRepository;
import com.flourishtravel.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminTourRosterServiceTest {

    @Mock
    private TourRepository tourRepository;
    @Mock
    private SessionWaitlistRepository waitlistRepository;
    @Mock
    private GuideService guideService;

    private AdminTourRosterService service;

    @BeforeEach
    void setUp() {
        service = new AdminTourRosterService(tourRepository, waitlistRepository, guideService);
    }

    @Test
    void waitlistMergesTourLevelAndSessionLevel() {
        UUID tourId = UUID.randomUUID();
        Tour tour = Tour.builder().title("Bangkok").build();
        tour.setId(tourId);
        when(tourRepository.findById(tourId)).thenReturn(Optional.of(tour));

        User u1 = User.builder().fullName("A").email("a@x.com").phone("1").build();
        u1.setId(UUID.randomUUID());
        SessionWaitlist tourLevel = SessionWaitlist.builder().user(u1).tour(tour).status("waiting").build();
        tourLevel.setId(UUID.randomUUID());
        tourLevel.setCreatedAt(Instant.parse("2026-01-02T00:00:00Z"));

        User u2 = User.builder().fullName("B").email("b@x.com").phone("2").build();
        u2.setId(UUID.randomUUID());
        TourSession session = TourSession.builder()
                .tour(tour)
                .startDate(LocalDate.of(2026, 8, 20))
                .endDate(LocalDate.of(2026, 8, 23))
                .build();
        session.setId(UUID.randomUUID());
        SessionWaitlist sessionLevel = SessionWaitlist.builder().user(u2).session(session).status("waiting").build();
        sessionLevel.setId(UUID.randomUUID());
        sessionLevel.setCreatedAt(Instant.parse("2026-01-03T00:00:00Z"));

        when(waitlistRepository.findTourLevelByTourId(tourId)).thenReturn(List.of(tourLevel));
        when(waitlistRepository.findSessionLevelByTourId(tourId)).thenReturn(List.of(sessionLevel));

        var rows = service.waitlistForTour(tourId);
        assertEquals(2, rows.size());
        assertEquals("B", rows.get(0).getFullName());
        assertEquals("session", rows.get(0).getScope());
        assertEquals("A", rows.get(1).getFullName());
        assertEquals("tour", rows.get(1).getScope());
    }
}
