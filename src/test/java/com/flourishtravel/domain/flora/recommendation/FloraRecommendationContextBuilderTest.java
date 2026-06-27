package com.flourishtravel.domain.flora.recommendation;

import com.flourishtravel.common.exception.BadRequestException;
import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.chatbot.client.OpenMeteoClient;
import com.flourishtravel.domain.flora.FloraRecommendationConstants;
import com.flourishtravel.domain.flora.dto.FloraActivityDto;
import com.flourishtravel.domain.flora.dto.FloraJourneyDto;
import com.flourishtravel.domain.flora.entity.UserTravelPreference;
import com.flourishtravel.domain.flora.recommendation.dto.FloraNearbyRecommendationRequest;
import com.flourishtravel.domain.flora.service.FloraPrivacyService;
import com.flourishtravel.domain.tour.entity.Tour;
import com.flourishtravel.domain.tour.entity.TourSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FloraRecommendationContextBuilderTest {

    @Mock FloraPrivacyService privacyService;
    @Mock OpenMeteoClient openMeteoClient;
    @InjectMocks FloraRecommendationContextBuilder builder;

    private FloraRecommendationProperties props;

    @BeforeEach
    void setUp() {
        props = new FloraRecommendationProperties();
    }

    @Test
    void validateRequest_rejectsInvalidLatitude() {
        FloraNearbyRecommendationRequest req = new FloraNearbyRecommendationRequest();
        req.setLatitude(95.0);
        req.setLongitude(106.0);
        assertThrows(BadRequestException.class, () -> builder.validateRequest(req, props));
    }

    @Test
    void withoutLocationConsent_ignoresUserGps() {
        UUID userId = UUID.randomUUID();
        when(privacyService.hasLocationConsent(userId)).thenReturn(false);

        FloraJourneyDto journey = FloraJourneyDto.builder()
                .currentActivity(FloraActivityDto.builder()
                        .latitude(10.77)
                        .longitude(106.70)
                        .locationName("Chợ Đà Lạt")
                        .build())
                .build();

        FloraNearbyRecommendationRequest req = new FloraNearbyRecommendationRequest();
        req.setLatitude(11.0);
        req.setLongitude(107.0);

        var resolved = builder.resolveLocation(userId, req, journey, new Booking());
        assertEquals(FloraRecommendationConstants.LOCATION_ACTIVITY, resolved.getSource());
        assertEquals(10.77, resolved.getLatitude());
    }

    @Test
    void activityLocationUsedWhenGpsUnavailable() {
        UUID userId = UUID.randomUUID();
        FloraJourneyDto journey = FloraJourneyDto.builder()
                .currentActivity(FloraActivityDto.builder()
                        .latitude(11.94)
                        .longitude(108.45)
                        .locationName("Đà Lạt")
                        .build())
                .build();

        var resolved = builder.resolveLocation(userId, new FloraNearbyRecommendationRequest(), journey, new Booking());
        assertEquals(FloraRecommendationConstants.LOCATION_ACTIVITY, resolved.getSource());
    }

    @Test
    void destinationFallbackMarkedClearly() {
        UUID userId = UUID.randomUUID();
        when(openMeteoClient.geocode("Đà Lạt Vietnam")).thenReturn(new double[] {11.94, 108.45});

        Tour tour = Tour.builder().destinationCity("Đà Lạt").build();
        TourSession session = TourSession.builder().tour(tour).build();
        Booking booking = Booking.builder().session(session).build();

        var resolved = builder.resolveLocation(userId, new FloraNearbyRecommendationRequest(), null, booking);
        assertEquals(FloraRecommendationConstants.LOCATION_DESTINATION, resolved.getSource());
        assertEquals("Đà Lạt", resolved.getLabel());
    }

    @Test
    void userLocationWhenRequestConsentWithoutProfile() {
        UUID userId = UUID.randomUUID();
        when(privacyService.hasLocationConsent(userId)).thenReturn(false);

        FloraNearbyRecommendationRequest req = new FloraNearbyRecommendationRequest();
        req.setLatitude(21.0285);
        req.setLongitude(105.8542);
        req.setLocationConsent(true);

        var resolved = builder.resolveLocation(userId, req, null, new Booking());
        assertEquals(FloraRecommendationConstants.LOCATION_USER, resolved.getSource());
        assertEquals(21.0285, resolved.getLatitude());
    }

    @Test
    void geocodeActivityLocationWhenNoCoordinates() {
        UUID userId = UUID.randomUUID();
        when(openMeteoClient.geocode("Khu công nghệ cao Hòa Lạc Vietnam"))
                .thenReturn(new double[] {21.013, 105.525});

        FloraJourneyDto journey = FloraJourneyDto.builder()
                .currentActivity(FloraActivityDto.builder()
                        .locationName("Khu công nghệ cao Hòa Lạc")
                        .build())
                .build();

        var resolved = builder.resolveLocation(userId, new FloraNearbyRecommendationRequest(), journey, new Booking());
        assertEquals(FloraRecommendationConstants.LOCATION_DESTINATION, resolved.getSource());
        assertEquals(21.013, resolved.getLatitude());
    }

    @Test
    void userLocationWhenConsentAndGps() {
        UUID userId = UUID.randomUUID();
        when(privacyService.hasLocationConsent(userId)).thenReturn(true);

        FloraNearbyRecommendationRequest req = new FloraNearbyRecommendationRequest();
        req.setLatitude(10.7769);
        req.setLongitude(106.7009);

        var resolved = builder.resolveLocation(userId, req, null, new Booking());
        assertEquals(FloraRecommendationConstants.LOCATION_USER, resolved.getSource());
        assertEquals(10.7769, resolved.getLatitude());
    }
}
