package com.flourishtravel.domain.chatbot.security;

import com.flourishtravel.common.exception.BadRequestException;
import com.flourishtravel.common.exception.ResourceNotFoundException;
import com.flourishtravel.domain.chatbot.dto.ChatbotRequest;
import com.flourishtravel.domain.chatbot.dto.ChatbotResponse;
import com.flourishtravel.domain.flora.FloraQuickActions;
import com.flourishtravel.domain.flora.service.FloraPrivacyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatbotSecurityService {

    public static final String ANONYMOUS_BOOKING_MESSAGE =
            "Bạn hãy đăng nhập để Flora có thể hỗ trợ theo thông tin chuyến đi của bạn nhé.";

    private final FloraPrivacyService privacyService;
    private final ChatbotSecurityAuditLogger auditLogger;

    public void validateCoordinates(Double latitude, Double longitude) {
        boolean hasLat = latitude != null;
        boolean hasLon = longitude != null;
        if (hasLat != hasLon) {
            throw new BadRequestException("latitude và longitude phải được gửi cùng nhau.");
        }
        if (hasLat) {
            if (latitude < -90.0 || latitude > 90.0 || longitude < -180.0 || longitude > 180.0) {
                throw new BadRequestException("Tọa độ GPS không hợp lệ.");
            }
        }
    }

    public Optional<ChatbotResponse> bookingAccessResponse(ChatbotRequest request, UUID authenticatedUserId) {
        if (request.getBookingId() == null) {
            return Optional.empty();
        }
        if (authenticatedUserId == null) {
            auditLogger.bookingContextDenied(false);
            return Optional.of(ChatbotResponse.builder()
                    .reply(ANONYMOUS_BOOKING_MESSAGE)
                    .quickReplies(FloraQuickActions.defaults())
                    .build());
        }
        return Optional.empty();
    }

    public ChatbotRequest prepareRequest(ChatbotRequest request, UUID authenticatedUserId) {
        validateCoordinates(request.getLatitude(), request.getLongitude());

        UUID bookingId = request.getBookingId();
        if (bookingId != null && authenticatedUserId != null) {
            try {
                privacyService.requireOwnedBooking(bookingId, authenticatedUserId);
                auditLogger.bookingContextRequested(authenticatedUserId);
            } catch (ResourceNotFoundException ex) {
                auditLogger.bookingContextDenied(true);
                request.setBookingId(null);
            }
        } else if (bookingId != null) {
            request.setBookingId(null);
        }

        if (request.getLatitude() != null && request.getLongitude() != null
                && authenticatedUserId != null
                && !privacyService.hasLocationConsent(authenticatedUserId)) {
            request.setLatitude(null);
            request.setLongitude(null);
        }

        return request;
    }

    public boolean shouldIncludeLocationInHint(ChatbotRequest request, UUID authenticatedUserId) {
        if (request.getLatitude() == null || request.getLongitude() == null) {
            return false;
        }
        if (authenticatedUserId == null) {
            return true;
        }
        return privacyService.hasLocationConsent(authenticatedUserId);
    }
}
