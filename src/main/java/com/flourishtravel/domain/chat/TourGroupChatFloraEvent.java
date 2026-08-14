package com.flourishtravel.domain.chat;

import java.util.UUID;

public record TourGroupChatFloraEvent(UUID bookingId, UUID askerUserId, String content) {
}
