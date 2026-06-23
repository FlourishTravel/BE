package com.flourishtravel.domain.chatbot.security;

import com.flourishtravel.common.exception.BadRequestException;
import com.flourishtravel.common.exception.ResourceNotFoundException;
import com.flourishtravel.domain.chatbot.dto.ChatbotRequest;
import com.flourishtravel.domain.flora.service.FloraPrivacyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatbotSecurityServiceTest {

    @Mock FloraPrivacyService privacyService;
    @Mock ChatbotSecurityAuditLogger auditLogger;
    @InjectMocks ChatbotSecurityService service;

    @Test
    void anonymousRequestWithBookingId_returnsLoginMessage() {
        ChatbotRequest request = new ChatbotRequest();
        request.setBookingId(UUID.randomUUID());
        request.setContent("help");

        var response = service.bookingAccessResponse(request, null);
        assertTrue(response.isPresent());
        assertEquals(ChatbotSecurityService.ANONYMOUS_BOOKING_MESSAGE, response.get().getReply());
        verify(auditLogger).bookingContextDenied(false);
    }

    @Test
    void authenticatedUser_cannotLoadAnotherUsersBooking() {
        UUID userId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        ChatbotRequest request = new ChatbotRequest();
        request.setBookingId(bookingId);
        request.setContent("trip");

        when(privacyService.requireOwnedBooking(bookingId, userId))
                .thenThrow(new ResourceNotFoundException("Booking", bookingId));

        ChatbotRequest prepared = service.prepareRequest(request, userId);
        assertNull(prepared.getBookingId());
        verify(auditLogger).bookingContextDenied(true);
    }

    @Test
    void invalidLatitude_rejected() {
        ChatbotRequest request = new ChatbotRequest();
        request.setLatitude(95.0);
        request.setLongitude(10.0);

        assertThrows(BadRequestException.class, () -> service.validateCoordinates(request.getLatitude(), request.getLongitude()));
    }

    @Test
    void partialCoordinates_rejected() {
        assertThrows(BadRequestException.class, () -> service.validateCoordinates(10.0, null));
    }

    @Test
    void authenticatedWithoutLocationConsent_stripsCoordinates() {
        UUID userId = UUID.randomUUID();
        ChatbotRequest request = new ChatbotRequest();
        request.setLatitude(16.0);
        request.setLongitude(108.0);
        when(privacyService.hasLocationConsent(userId)).thenReturn(false);

        ChatbotRequest prepared = service.prepareRequest(request, userId);
        assertNull(prepared.getLatitude());
        assertNull(prepared.getLongitude());
    }
}
