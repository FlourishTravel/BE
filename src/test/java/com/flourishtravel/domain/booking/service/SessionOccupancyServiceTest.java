package com.flourishtravel.domain.booking.service;

import com.flourishtravel.domain.booking.repository.BookingRepository;
import com.flourishtravel.domain.tour.entity.TourSession;
import com.flourishtravel.domain.tour.repository.TourSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionOccupancyServiceTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private TourSessionRepository tourSessionRepository;

    private SessionOccupancyService occupancy;

    @BeforeEach
    void setUp() {
        occupancy = new SessionOccupancyService(bookingRepository, tourSessionRepository);
    }

    @Test
    void treatsNullSumAsZero() {
        UUID sessionId = UUID.randomUUID();
        when(bookingRepository.sumHeldGuestCount(sessionId)).thenReturn(null);
        assertEquals(0, occupancy.heldSeats(sessionId));
    }

    @Test
    void mapsHeldSeatsBySession() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        Object[] row = new Object[]{a, 23L};
        when(bookingRepository.sumHeldGuestCountBySessionIds(List.of(a, b)))
                .thenReturn(java.util.Collections.singletonList(row));
        Map<UUID, Integer> held = occupancy.heldSeatsBySessionIds(List.of(a, b));
        assertEquals(23, held.get(a));
        assertEquals(null, held.get(b));
        assertEquals(0, held.getOrDefault(b, 0));
    }

    @Test
    void syncWritesLiveSumOntoSession() {
        UUID sessionId = UUID.randomUUID();
        TourSession session = TourSession.builder()
                .currentParticipants(20)
                .maxParticipants(20)
                .build();
        session.setId(sessionId);
        when(bookingRepository.sumHeldGuestCount(sessionId)).thenReturn(23L);
        when(tourSessionRepository.save(session)).thenReturn(session);

        assertEquals(23, occupancy.sync(session));
        assertEquals(23, session.getCurrentParticipants());
        verify(tourSessionRepository).save(session);
    }

    @Test
    void syncAllSkipsUnchanged() {
        UUID sessionId = UUID.randomUUID();
        TourSession session = TourSession.builder()
                .currentParticipants(6)
                .maxParticipants(20)
                .build();
        session.setId(sessionId);
        when(tourSessionRepository.findAll()).thenReturn(List.of(session));
        Object[] row = new Object[]{sessionId, 6L};
        when(bookingRepository.sumHeldGuestCountBySessionIds(List.of(sessionId)))
                .thenReturn(java.util.Collections.singletonList(row));

        assertEquals(0, occupancy.syncAll());
        verify(tourSessionRepository, never()).save(any());
    }
}
