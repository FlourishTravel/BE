package com.flourishtravel.domain.flora.recommendation;

import com.flourishtravel.common.exception.ResourceNotFoundException;
import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.chatbot.client.OverpassClient;
import com.flourishtravel.domain.chatbot.service.ChatbotDataService;
import com.flourishtravel.domain.flora.FloraRecommendationConstants;
import com.flourishtravel.domain.flora.FloraScheduleConstants;
import com.flourishtravel.domain.flora.dto.FloraActivityDto;
import com.flourishtravel.domain.flora.dto.FloraJourneyDto;
import com.flourishtravel.domain.flora.dto.FloraNextMeetingDto;
import com.flourishtravel.domain.flora.recommendation.dto.FloraNearbyRecommendationRequest;
import com.flourishtravel.domain.flora.service.FloraJourneyService;
import com.flourishtravel.domain.flora.service.FloraPrivacyService;
import com.flourishtravel.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FloraNearbyRecommendationServiceTest {

    @Mock FloraPrivacyService privacyService;
    @Mock FloraJourneyService journeyService;
    @Mock FloraRecommendationContextBuilder contextBuilder;
    @Mock OverpassClient overpassClient;
    @Mock ChatbotDataService chatbotDataService;
    @InjectMocks FloraNearbyRecommendationService service;

    private FloraRecommendationProperties properties;
    private UUID bookingId;
    private UUID userId;
    private UUID otherUserId;

    @BeforeEach
    void setUp() {
        properties = new FloraRecommendationProperties();
        service = new FloraNearbyRecommendationService(
                privacyService, journeyService, contextBuilder, properties, overpassClient, chatbotDataService);
        bookingId = UUID.randomUUID();
        userId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();
    }

    @Test
    void cannotAccessAnotherUsersBooking() {
        when(privacyService.requireOwnedBooking(bookingId, otherUserId))
                .thenThrow(new ResourceNotFoundException("Booking", bookingId));

        assertThrows(ResourceNotFoundException.class,
                () -> service.recommend(bookingId, otherUserId, new FloraNearbyRecommendationRequest()));
    }

    @Test
    void noConfirmedMeeting_canValidateScheduleFalse() {
        Booking booking = Booking.builder().build();
        FloraJourneyDto journey = FloraJourneyDto.builder()
                .nextMeeting(FloraNextMeetingDto.builder()
                        .scheduleStatus(FloraScheduleConstants.SCHEDULE_ESTIMATED)
                        .time(Instant.parse("2026-06-25T03:40:00Z"))
                        .locationName("Cổng")
                        .build())
                .freeMinutesUntilMeeting(30L)
                .build();

        stubLocationAndJourney(booking, journey, 10.77, 106.70, FloraRecommendationConstants.LOCATION_ACTIVITY);
        when(overpassClient.findNearbyPois(anyDouble(), anyDouble(), anyInt(), anyList(), anyInt()))
                .thenReturn(samplePoi());

        var response = service.recommend(bookingId, userId, new FloraNearbyRecommendationRequest());
        assertFalse(response.getJourneyContext().getCanValidateSchedule());
        assertFalse(response.getRecommendations().get(0).getFitsSchedule());
    }

    @Test
    void confirmedMeetingEnoughTime_fitsScheduleTrue() {
        Booking booking = Booking.builder().build();
        FloraJourneyDto journey = FloraJourneyDto.builder()
                .nextMeeting(FloraNextMeetingDto.builder()
                        .scheduleStatus(FloraScheduleConstants.SCHEDULE_CONFIRMED)
                        .time(Instant.parse("2026-06-25T03:40:00Z"))
                        .locationName("Bãi xe")
                        .build())
                .freeMinutesUntilMeeting(120L)
                .build();

        stubLocationAndJourney(booking, journey, 10.77, 106.70, FloraRecommendationConstants.LOCATION_ACTIVITY);
        when(overpassClient.findNearbyPois(anyDouble(), anyDouble(), anyInt(), anyList(), anyInt()))
                .thenReturn(samplePoi());

        var response = service.recommend(bookingId, userId, new FloraNearbyRecommendationRequest());
        assertTrue(response.getJourneyContext().getCanValidateSchedule());
        assertTrue(response.getRecommendations().get(0).getFitsSchedule());
    }

    @Test
    void unavailableLocation_returnsEmptyRecommendations() {
        Booking booking = Booking.builder().build();
        when(privacyService.requireOwnedBooking(bookingId, userId)).thenReturn(booking);
        when(journeyService.getJourney(bookingId, userId)).thenReturn(FloraJourneyDto.builder().build());
        when(contextBuilder.resolveLocation(eq(userId), any(), any(), eq(booking)))
                .thenReturn(FloraRecommendationContextBuilder.ResolvedLocation.builder()
                        .source(FloraRecommendationConstants.LOCATION_UNAVAILABLE)
                        .build());

        var response = service.recommend(bookingId, userId, new FloraNearbyRecommendationRequest());
        assertTrue(response.getRecommendations().isEmpty());
        verifyNoInteractions(overpassClient);
    }

    @Test
    void recommendationsIncludeOsmDataSourceAndTravelWarning() {
        Booking booking = Booking.builder().build();
        FloraJourneyDto journey = FloraJourneyDto.builder()
                .currentActivity(FloraActivityDto.builder().title("Chợ").latitude(10.77).longitude(106.70).build())
                .build();
        stubLocationAndJourney(booking, journey, 10.77, 106.70, FloraRecommendationConstants.LOCATION_USER);
        when(overpassClient.findNearbyPois(anyDouble(), anyDouble(), anyInt(), anyList(), anyInt()))
                .thenReturn(samplePoi());

        var response = service.recommend(bookingId, userId, new FloraNearbyRecommendationRequest());
        var item = response.getRecommendations().get(0);
        assertEquals(FloraRecommendationConstants.SOURCE_OSM, item.getDataSource());
        assertEquals(FloraRecommendationConstants.BUDGET_UNKNOWN, item.getBudgetMatchStatus());
        assertTrue(item.getWarnings().stream()
                .anyMatch(w -> w.contains("ước tính theo khoảng cách thẳng")));
    }

    private void stubLocationAndJourney(
            Booking booking, FloraJourneyDto journey, double lat, double lon, String source) {
        when(privacyService.requireOwnedBooking(bookingId, userId)).thenReturn(booking);
        when(journeyService.getJourney(bookingId, userId)).thenReturn(journey);
        when(privacyService.getPreferencesOrDefault(userId)).thenReturn(null);
        when(privacyService.hasPersonalizationConsent(userId)).thenReturn(false);
        when(contextBuilder.resolveLocation(eq(userId), any(), eq(journey), eq(booking)))
                .thenReturn(FloraRecommendationContextBuilder.ResolvedLocation.builder()
                        .latitude(lat)
                        .longitude(lon)
                        .source(source)
                        .label("Test")
                        .build());
        doNothing().when(contextBuilder).validateRequest(any(), eq(properties));
    }

    private static List<Map<String, Object>> samplePoi() {
        return List.of(Map.of(
                "type", "node",
                "id", 123456L,
                "lat", 10.771,
                "lon", 106.701,
                "tags", Map.of("name", "Quán cà phê A", "amenity", "cafe")));
    }
}
