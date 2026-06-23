package com.flourishtravel.domain.flora.service;

import com.flourishtravel.common.exception.ForbiddenException;
import com.flourishtravel.common.exception.ResourceNotFoundException;
import com.flourishtravel.domain.booking.entity.Booking;
import com.flourishtravel.domain.booking.repository.BookingRepository;
import com.flourishtravel.domain.flora.entity.UserTravelPreference;
import com.flourishtravel.domain.flora.repository.UserTravelPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FloraPrivacyService {

    private final BookingRepository bookingRepository;
    private final UserTravelPreferenceRepository preferenceRepository;

    @Transactional(readOnly = true)
    public Booking requireOwnedBooking(UUID bookingId, UUID userId) {
        Booking booking = bookingRepository.findDetailForUser(bookingId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
        if (!booking.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Booking", bookingId);
        }
        return booking;
    }

    @Transactional(readOnly = true)
    public UserTravelPreference getPreferencesOrDefault(UUID userId) {
        return preferenceRepository.findByUserId(userId).orElse(null);
    }

    @Transactional(readOnly = true)
    public void requireLocationConsent(UUID userId) {
        UserTravelPreference pref = preferenceRepository.findByUserId(userId).orElse(null);
        if (pref == null || !Boolean.TRUE.equals(pref.getLocationConsent())) {
            throw new ForbiddenException("Bạn cần bật quyền vị trí trong cài đặt Flora để sử dụng tính năng này.");
        }
    }

    @Transactional(readOnly = true)
    public boolean hasPersonalizationConsent(UUID userId) {
        UserTravelPreference pref = preferenceRepository.findByUserId(userId).orElse(null);
        return pref == null || Boolean.TRUE.equals(pref.getPersonalizationConsent());
    }

    @Transactional(readOnly = true)
    public boolean hasNotificationConsent(UUID userId) {
        UserTravelPreference pref = preferenceRepository.findByUserId(userId).orElse(null);
        return pref == null || Boolean.TRUE.equals(pref.getNotificationConsent());
    }

    @Transactional(readOnly = true)
    public boolean hasLocationConsent(UUID userId) {
        UserTravelPreference pref = preferenceRepository.findByUserId(userId).orElse(null);
        return pref != null && Boolean.TRUE.equals(pref.getLocationConsent());
    }

    /** Loại bỏ thông tin nhạy cảm trước khi đưa vào prompt LLM. */
    public String sanitizeForPrompt(String text) {
        if (text == null || text.isBlank()) return "";
        return text
                .replaceAll("(?i)\\b\\d{9,11}\\b", "[SĐT]")
                .replaceAll("(?i)[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}", "[email]");
    }

    /** Mở rộng sanitizeForPrompt — loại số tiền, refund và UUID nội bộ khỏi ngữ cảnh LLM. */
    public String sanitizeForLlm(String text) {
        if (text == null || text.isBlank()) return "";
        String sanitized = sanitizeForPrompt(text);
        sanitized = sanitized.replaceAll("(?i)~\\d+\\s*triệu", "[số tiền]");
        sanitized = sanitized.replaceAll("(?i)(thanh toán|hoàn tiền|refund|payment)[^\\n]*", "[thanh toán]");
        sanitized = sanitized.replaceAll(
                "(?i)\\b[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\b",
                "[id]");
        return sanitized;
    }
}
