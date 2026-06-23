package com.flourishtravel.domain.tour.service;

import com.flourishtravel.common.exception.BadRequestException;
import com.flourishtravel.common.exception.ForbiddenException;
import com.flourishtravel.domain.flora.FloraScheduleConstants;
import com.flourishtravel.domain.flora.service.FloraScheduleChangeNotifier;
import com.flourishtravel.domain.guide.dto.GuideSessionDetailDto;
import com.flourishtravel.domain.guide.service.GuideService;
import com.flourishtravel.domain.tour.SessionScheduleConstants;
import com.flourishtravel.domain.tour.dto.SessionActivitySchedulePatchRequest;
import com.flourishtravel.domain.tour.entity.Tour;
import com.flourishtravel.domain.tour.entity.TourActivity;
import com.flourishtravel.domain.tour.entity.TourItinerary;
import com.flourishtravel.domain.tour.entity.TourSession;
import com.flourishtravel.domain.tour.entity.TourSessionActivityOverride;
import com.flourishtravel.domain.tour.repository.TourActivityRepository;
import com.flourishtravel.domain.tour.repository.TourRepository;
import com.flourishtravel.domain.tour.repository.TourSessionActivityOverrideRepository;
import com.flourishtravel.domain.tour.repository.TourSessionRepository;
import com.flourishtravel.domain.user.entity.Role;
import com.flourishtravel.domain.user.entity.User;
import com.flourishtravel.domain.user.repository.UserRepository;
import com.flourishtravel.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TourSessionScheduleServiceTest {

    @Mock TourSessionRepository sessionRepository;
    @Mock TourRepository tourRepository;
    @Mock TourActivityRepository activityRepository;
    @Mock TourSessionActivityOverrideRepository overrideRepository;
    @Mock UserRepository userRepository;
    @Mock GuideService guideService;
    @Mock FloraScheduleChangeNotifier scheduleChangeNotifier;
    @InjectMocks TourSessionScheduleService service;

    private UUID sessionId;
    private UUID activityId;
    private UUID guideUserId;
    private TourSession session;
    private TourActivity activity;
    private Tour tour;
    private UserPrincipal guidePrincipal;
    private UserPrincipal adminPrincipal;

    @BeforeEach
    void setUp() {
        sessionId = UUID.randomUUID();
        activityId = UUID.randomUUID();
        guideUserId = UUID.randomUUID();
        tour = Tour.builder().title("Tour").build();
        tour.setId(UUID.randomUUID());
        session = TourSession.builder().tour(tour).startDate(LocalDate.of(2026, 7, 10)).build();
        session.setId(sessionId);
        activity = TourActivity.builder()
                .title("Tập trung")
                .startTime(LocalTime.of(10, 40))
                .endTime(LocalTime.of(10, 50))
                .locationName("Bãi xe")
                .isGatheringEvent(true)
                .scheduleStatus(FloraScheduleConstants.SCHEDULE_CONFIRMED)
                .build();
        activity.setId(activityId);
        TourItinerary day = TourItinerary.builder().dayNumber(1).activities(List.of(activity)).build();
        tour.setItineraries(List.of(day));

        guidePrincipal = principalFor(guideUserId, "TOUR_GUIDE");
        adminPrincipal = principalFor(UUID.randomUUID(), "ADMIN");
    }

    private static UserPrincipal principalFor(UUID id, String roleName) {
        Role role = Role.builder().name(roleName).build();
        User user = User.builder().email(roleName + "@test.com").isActive(true).role(role).build();
        user.setId(id);
        return new UserPrincipal(user);
    }

    @Test
    void loadPublishedOverrides_ignoresDraft() {
        TourSessionActivityOverride draft = TourSessionActivityOverride.builder()
                .publicationStatus(SessionScheduleConstants.PUBLICATION_DRAFT)
                .tourActivity(activity)
                .build();
        when(overrideRepository.findByTourSession_Id(sessionId)).thenReturn(List.of(draft));

        Map<UUID, TourSessionActivityOverride> map = service.loadPublishedOverrides(sessionId);
        assertTrue(map.isEmpty());
    }

    @Test
    void saveDraft_doesNotMutateTemplateActivity() {
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(guideService.getSessionById(sessionId, guideUserId)).thenReturn(GuideSessionDetailDto.builder().build());
        when(activityRepository.existsForTour(activityId, tour.getId())).thenReturn(true);
        when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));
        when(overrideRepository.findByTourSession_IdAndTourActivity_Id(sessionId, activityId)).thenReturn(Optional.empty());
        when(overrideRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(overrideRepository.findByTourSession_Id(sessionId)).thenReturn(List.of());
        when(tourRepository.findByIdWithItinerariesAndActivities(tour.getId())).thenReturn(Optional.of(tour));

        SessionActivitySchedulePatchRequest patch = new SessionActivitySchedulePatchRequest();
        patch.setStartAt(OffsetDateTime.of(2026, 7, 10, 10, 20, 0, 0, ZoneOffset.ofHours(7)));
        patch.setLocationName("Cổng phụ");
        patch.setScheduleStatus(FloraScheduleConstants.SCHEDULE_CONFIRMED);

        service.saveDraft(sessionId, activityId, patch, guidePrincipal);

        assertEquals(LocalTime.of(10, 40), activity.getStartTime());
        ArgumentCaptor<TourSessionActivityOverride> captor = ArgumentCaptor.forClass(TourSessionActivityOverride.class);
        verify(overrideRepository).save(captor.capture());
        assertEquals(SessionScheduleConstants.PUBLICATION_DRAFT, captor.getValue().getPublicationStatus());
        assertEquals(LocalTime.of(10, 20), captor.getValue().getStartTimeOverride());
    }

    @Test
    void guideCannotManageOtherGuideSession() {
        when(guideService.getSessionById(sessionId, guideUserId))
                .thenThrow(new ForbiddenException("Không phải HDV của chuyến này"));

        assertThrows(ForbiddenException.class,
                () -> service.getSchedule(sessionId, guidePrincipal));
    }

    @Test
    void adminCanManageAnySession() {
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(overrideRepository.findByTourSession_Id(sessionId)).thenReturn(List.of());
        when(tourRepository.findByIdWithItinerariesAndActivities(tour.getId())).thenReturn(Optional.of(tour));

        assertNotNull(service.getSchedule(sessionId, adminPrincipal));
        verify(guideService, never()).getSessionById(any(), any());
    }

    @Test
    void publish_requiresDraft() {
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(guideService.getSessionById(sessionId, guideUserId)).thenReturn(GuideSessionDetailDto.builder().build());
        when(activityRepository.existsForTour(activityId, tour.getId())).thenReturn(true);
        when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));
        when(overrideRepository.findByTourSession_IdAndTourActivity_Id(sessionId, activityId)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class,
                () -> service.publish(sessionId, activityId, guidePrincipal));
    }
}
