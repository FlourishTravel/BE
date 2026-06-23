package com.flourishtravel.domain.notification.push.service;

import com.flourishtravel.common.exception.BadRequestException;
import com.flourishtravel.common.exception.ResourceNotFoundException;
import com.flourishtravel.domain.flora.service.FloraPrivacyService;
import com.flourishtravel.domain.notification.push.PushPlatform;
import com.flourishtravel.domain.notification.push.PushTokenHasher;
import com.flourishtravel.domain.notification.push.config.FcmPushProperties;
import com.flourishtravel.domain.notification.push.dto.PushDeviceRegisterRequest;
import com.flourishtravel.domain.notification.push.dto.PushDeviceRegisterResponse;
import com.flourishtravel.domain.notification.push.dto.PushDeviceStatusDto;
import com.flourishtravel.domain.notification.push.entity.PushDevice;
import com.flourishtravel.domain.notification.push.repository.PushDeviceRepository;
import com.flourishtravel.domain.user.entity.User;
import com.flourishtravel.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushDeviceService {

    private final PushDeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final FloraPrivacyService privacyService;
    private final FcmPushProperties properties;

    private final ConcurrentHashMap<UUID, Instant> lastRegisterAttempt = new ConcurrentHashMap<>();

    @Transactional
    public PushDeviceRegisterResponse register(UUID userId, PushDeviceRegisterRequest request) {
        throttleRegistration(userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        String token = request.getToken().trim();
        String tokenHash = PushTokenHasher.hash(token);
        String platform = normalizePlatform(request.getPlatform());
        boolean permissionGranted = Boolean.TRUE.equals(request.getNotificationPermissionGranted());

        PushDevice device = deviceRepository.findByTokenHash(tokenHash).orElse(null);
        if (device == null) {
            enforceDeviceCap(user);
            device = PushDevice.builder()
                    .user(user)
                    .tokenCiphertext(token)
                    .tokenHash(tokenHash)
                    .platform(platform)
                    .build();
        } else {
            device.setUser(user);
            device.setTokenCiphertext(token);
            device.setActive(true);
        }
        device.setAppVersion(trimOrNull(request.getAppVersion()));
        device.setNotificationPermissionGranted(permissionGranted);
        device.setLastSeenAt(Instant.now());
        deviceRepository.save(device);

        boolean consent = privacyService.hasNotificationConsent(userId);
        boolean pushEnabled = consent && permissionGranted && properties.isEnabled();

        return PushDeviceRegisterResponse.builder()
                .registered(true)
                .pushEnabled(pushEnabled)
                .devicePermissionGranted(permissionGranted)
                .build();
    }

    @Transactional
    public void unregister(UUID userId, String token) {
        if (token == null || token.isBlank()) {
            throw new BadRequestException("Token không hợp lệ");
        }
        String tokenHash = PushTokenHasher.hash(token.trim());
        deviceRepository.deactivateByUserAndTokenHash(userId, tokenHash);
    }

    @Transactional(readOnly = true)
    public PushDeviceStatusDto status(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        List<PushDevice> active = deviceRepository.findByUserAndActiveTrue(user);
        boolean permissionGranted = active.stream()
                .anyMatch(d -> Boolean.TRUE.equals(d.getNotificationPermissionGranted()));
        boolean consent = privacyService.hasNotificationConsent(userId);
        return PushDeviceStatusDto.builder()
                .pushEnabled(consent && permissionGranted && properties.isEnabled() && !active.isEmpty())
                .notificationConsent(consent)
                .devicePermissionGranted(permissionGranted)
                .activeDeviceCount(active.size())
                .build();
    }

    private void enforceDeviceCap(User user) {
        List<PushDevice> active = deviceRepository.findByUserAndActiveTrue(user);
        int max = Math.max(1, properties.getMaxDevicesPerUser());
        if (active.size() < max) return;
        active.stream()
                .sorted(Comparator.comparing(PushDevice::getLastSeenAt, Comparator.nullsFirst(Comparator.naturalOrder())))
                .limit(active.size() - max + 1L)
                .forEach(d -> {
                    d.setActive(false);
                    deviceRepository.save(d);
                });
    }

    private void throttleRegistration(UUID userId) {
        Instant now = Instant.now();
        Instant last = lastRegisterAttempt.put(userId, now);
        if (last != null && last.isAfter(now.minusSeconds(5))) {
            throw new BadRequestException("Đăng ký thiết bị quá nhanh, vui lòng thử lại sau");
        }
    }

    private static String normalizePlatform(String platform) {
        if (platform == null || platform.isBlank()) return PushPlatform.ANDROID;
        String p = platform.trim().toUpperCase(Locale.ROOT);
        if (!PushPlatform.ANDROID.equals(p)) {
            throw new BadRequestException("platform không được hỗ trợ");
        }
        return p;
    }

    private static String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
