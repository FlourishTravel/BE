package com.flourishtravel.domain.tour.service;

import com.flourishtravel.common.exception.BadRequestException;
import com.flourishtravel.domain.tour.client.GoongClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeocodeServiceTest {

    @Mock
    private GoongClient goongClient;

    @InjectMocks
    private GeocodeService geocodeService;

    @Test
    void throwsWhenApiKeyMissing() {
        when(goongClient.isConfigured()).thenReturn(false);
        assertThrows(BadRequestException.class, () ->
                geocodeService.resolveActivityCoordinates("Hồ Gươm", null, null));
    }

    @Test
    void resolvesFromLocationName() {
        when(goongClient.isConfigured()).thenReturn(true);
        when(goongClient.geocode("Hồ Gươm"))
                .thenReturn(Optional.of(new GoongClient.GoongGeocodeHit(21.0285, 105.852, "Hồ Gươm")));

        var result = geocodeService.resolveActivityCoordinates("Hồ Gươm", null, null);

        assertEquals(21.0285, result.getLatitude());
        assertEquals(105.852, result.getLongitude());
        assertEquals("goong", result.getProvider());
    }
}
