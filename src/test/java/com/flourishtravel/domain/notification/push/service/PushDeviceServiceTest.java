package com.flourishtravel.domain.notification.push.service;

import com.flourishtravel.common.exception.BadRequestException;
import com.flourishtravel.domain.flora.service.FloraPrivacyService;
import com.flourishtravel.domain.notification.push.PushPlatform;
import com.flourishtravel.domain.notification.push.config.FcmPushProperties;
import com.flourishtravel.domain.notification.push.dto.PushDeviceRegisterRequest;
import com.flourishtravel.domain.notification.push.entity.PushDevice;
import com.flourishtravel.domain.notification.push.repository.PushDeviceRepository;
import com.flourishtravel.domain.user.entity.User;
import com.flourishtravel.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PushDeviceServiceTest {

    @Mock PushDeviceRepository deviceRepository;
    @Mock UserRepository userRepository;
    @Mock FloraPrivacyService privacyService;
    @InjectMocks PushDeviceService pushDeviceService;

    private FcmPushProperties properties;
    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        properties = new FcmPushProperties();
        properties.setEnabled(true);
        properties.setMaxDevicesPerUser(5);
        pushDeviceService = new PushDeviceService(deviceRepository, userRepository, privacyService, properties);
        userId = UUID.randomUUID();
        user = User.builder().email("t@t.com").build();
        user.setId(userId);
    }

    @Test
    void register_doesNotReturnRawToken() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(deviceRepository.findByTokenHash(any())).thenReturn(Optional.empty());
        when(deviceRepository.findByUserAndActiveTrue(user)).thenReturn(List.of());
        when(privacyService.hasNotificationConsent(userId)).thenReturn(true);
        when(deviceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PushDeviceRegisterRequest req = new PushDeviceRegisterRequest();
        req.setToken("a".repeat(140));
        req.setPlatform(PushPlatform.ANDROID);
        req.setNotificationPermissionGranted(true);

        var res = pushDeviceService.register(userId, req);
        assertTrue(res.isRegistered());
        assertTrue(res.isPushEnabled());

        ArgumentCaptor<PushDevice> captor = ArgumentCaptor.forClass(PushDevice.class);
        verify(deviceRepository).save(captor.capture());
        assertNotNull(captor.getValue().getTokenHash());
    }

    @Test
    void register_reassignsTokenFromPreviousUser() {
        UUID otherId = UUID.randomUUID();
        User other = User.builder().email("o@o.com").build();
        other.setId(otherId);
        PushDevice existing = PushDevice.builder().user(other).tokenHash("hash").active(true).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(deviceRepository.findByTokenHash(any())).thenReturn(Optional.of(existing));
        when(privacyService.hasNotificationConsent(userId)).thenReturn(true);
        when(deviceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PushDeviceRegisterRequest req = new PushDeviceRegisterRequest();
        req.setToken("b".repeat(140));
        req.setPlatform(PushPlatform.ANDROID);
        req.setNotificationPermissionGranted(true);

        pushDeviceService.register(userId, req);
        assertEquals(userId, existing.getUser().getId());
    }

    @Test
    void unregister_deactivatesByHash() {
        when(deviceRepository.deactivateByUserAndTokenHash(eq(userId), any())).thenReturn(1);
        pushDeviceService.unregister(userId, "c".repeat(140));
        verify(deviceRepository).deactivateByUserAndTokenHash(eq(userId), any());
    }

    @Test
    void register_enforcesDeviceCap() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(deviceRepository.findByTokenHash(any())).thenReturn(Optional.empty());
        PushDevice old = PushDevice.builder().user(user).active(true).build();
        when(deviceRepository.findByUserAndActiveTrue(user)).thenReturn(List.of(old, old, old, old, old));
        when(privacyService.hasNotificationConsent(userId)).thenReturn(true);
        when(deviceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PushDeviceRegisterRequest req = new PushDeviceRegisterRequest();
        req.setToken("d".repeat(140));
        req.setPlatform(PushPlatform.ANDROID);
        req.setNotificationPermissionGranted(true);

        pushDeviceService.register(userId, req);
        verify(deviceRepository, atLeast(2)).save(any());
    }

    @Test
    void register_rejectsInvalidPlatform() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        PushDeviceRegisterRequest req = new PushDeviceRegisterRequest();
        req.setToken("e".repeat(140));
        req.setPlatform("IOS");
        assertThrows(BadRequestException.class, () -> pushDeviceService.register(userId, req));
    }
}
