package com.flourishtravel.domain.flora.service;

import com.flourishtravel.common.exception.ForbiddenException;
import com.flourishtravel.common.exception.ResourceNotFoundException;
import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.booking.repository.BookingRepository;
import com.flourishtravel.domain.flora.entity.UserTravelPreference;
import com.flourishtravel.domain.flora.repository.UserTravelPreferenceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FloraPrivacyServiceTest {

    @Mock BookingRepository bookingRepository;
    @Mock UserTravelPreferenceRepository preferenceRepository;
    @InjectMocks FloraPrivacyService service;

    @Test
    void requireOwnedBooking_deniesOtherUser() {
        UUID bookingId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        when(bookingRepository.findDetailForUser(bookingId, otherId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.requireOwnedBooking(bookingId, otherId));
    }

    @Test
    void requireLocationConsent_throwsWhenDisabled() {
        UUID userId = UUID.randomUUID();
        UserTravelPreference pref = UserTravelPreference.builder().locationConsent(false).build();
        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(pref));

        assertThrows(ForbiddenException.class, () -> service.requireLocationConsent(userId));
    }
}
