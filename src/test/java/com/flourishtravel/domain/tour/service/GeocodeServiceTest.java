package com.flourishtravel.domain.tour.service;

import com.flourishtravel.common.exception.BadRequestException;
import com.flourishtravel.domain.tour.client.VietMapClient;
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
    private VietMapClient vietMapClient;

    @InjectMocks
    private GeocodeService geocodeService;

    @Test
    void throwsWhenApiKeyMissing() {
        when(vietMapClient.isConfigured()).thenReturn(false);
        assertThrows(BadRequestException.class, () ->
                geocodeService.resolveActivityCoordinates("Hồ Gươm", null, null));
    }

    @Test
    void resolvesFromLocationName() {
        when(vietMapClient.isConfigured()).thenReturn(true);
        when(vietMapClient.geocode("Hồ Gươm"))
                .thenReturn(Optional.of(new VietMapClient.VietMapGeocodeHit(21.0285, 105.852, "Hồ Gươm")));

        var result = geocodeService.resolveActivityCoordinates("Hồ Gươm", null, null);

        assertEquals(21.0285, result.getLatitude());
        assertEquals(105.852, result.getLongitude());
        assertEquals("vietmap", result.getProvider());
    }
}
