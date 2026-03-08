package com.flourishtravel.domain.auth.service;

import com.flourishtravel.common.exception.BadRequestException;
import com.flourishtravel.common.exception.ResourceNotFoundException;
import com.flourishtravel.domain.auth.dto.AuthResponse;
import com.flourishtravel.domain.auth.dto.LoginRequest;
import com.flourishtravel.domain.auth.dto.RegisterRequest;
import com.flourishtravel.domain.user.entity.PasswordResetToken;
import com.flourishtravel.domain.user.entity.RefreshToken;
import com.flourishtravel.domain.user.entity.Role;
import com.flourishtravel.domain.user.entity.User;
import com.flourishtravel.domain.user.entity.UserProvider;
import com.flourishtravel.domain.user.repository.PasswordResetTokenRepository;
import com.flourishtravel.domain.user.repository.RefreshTokenRepository;
import com.flourishtravel.domain.user.repository.RoleRepository;
import com.flourishtravel.domain.user.repository.UserProviderRepository;
import com.flourishtravel.domain.user.repository.UserRepository;
import com.flourishtravel.security.JwtProvider;
import com.flourishtravel.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final UserProviderRepository userProviderRepository;

    private static final String TRAVELER_ROLE = "TRAVELER";
    private static final int RESET_TOKEN_EXPIRE_MINUTES = 60;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email đã được sử dụng");
        }
        Role role = roleRepository.findByName(TRAVELER_ROLE)
                .orElseGet(() -> roleRepository.save(Role.builder().name(TRAVELER_ROLE).description("Khách hàng").build()));
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .role(role)
                .isActive(true)
                .build();
        user = userRepository.save(user);
        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Email hoặc mật khẩu không đúng"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Email hoặc mật khẩu không đúng");
        }
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new BadCredentialsException("Tài khoản đã bị khóa");
        }
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        String hash = hashToken(refreshToken);
        RefreshToken rt = refreshTokenRepository.findByTokenHashAndRevokedFalse(hash)
                .orElseThrow(() -> new BadRequestException("Refresh token không hợp lệ"));
        if (rt.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Refresh token đã hết hạn");
        }
        User user = rt.getUser();
        rt.setRevoked(true);
        refreshTokenRepository.save(rt);
        return buildAuthResponse(user);
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) return;
        String hash = hashToken(refreshToken);
        refreshTokenRepository.findByTokenHashAndRevokedFalse(hash).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
    }

    /** Luồng 1.6: Quên mật khẩu. Luôn trả 200 để không lộ email có tồn tại. */
    @Transactional
    public void forgotPassword(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String rawToken = UUID.randomUUID().toString() + "-" + Instant.now().toEpochMilli();
            String tokenHash = hashToken(rawToken);
            passwordResetTokenRepository.save(PasswordResetToken.builder()
                    .user(user)
                    .tokenHash(tokenHash)
                    .expiresAt(Instant.now().plusSeconds(60L * RESET_TOKEN_EXPIRE_MINUTES))
                    .used(false)
                    .build());
            // TODO: Gửi email chứa link reset với token = rawToken (vd: /reset-password?token=...)
            // MailSender.send(user.getEmail(), "Reset password", "Link: " + frontendUrl + "/reset-password?token=" + rawToken);
        });
    }

    /** Luồng 1.6: Đặt lại mật khẩu từ token trong email. */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        String tokenHash = hashToken(token);
        PasswordResetToken prt = passwordResetTokenRepository
                .findByTokenHashAndUsedFalseAndExpiresAtAfter(tokenHash, Instant.now())
                .orElseThrow(() -> new BadRequestException("Token không hợp lệ hoặc đã hết hạn"));
        User user = prt.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        prt.setUsed(true);
        passwordResetTokenRepository.save(prt);
    }

    /** Luồng 1.3: Đăng nhập OAuth (Google/Facebook). id_token hoặc code (code cần đổi lấy token bên ngoài). */
    @Transactional
    public AuthResponse oauthLogin(String provider, String idToken, String code) {
        if (!"google".equals(provider) && !"facebook".equals(provider)) {
            throw new BadRequestException("Provider chỉ hỗ trợ google hoặc facebook");
        }
        String providerUserId;
        String email;
        String fullName;
        if (StringUtils.hasText(idToken)) {
            Map<String, Object> claims = decodeIdTokenPayload(idToken);
            providerUserId = (String) claims.get("sub");
            email = (String) claims.get("email");
            fullName = (String) claims.get("name");
            if (email == null) email = (String) claims.get("email_address");
            if (fullName == null) fullName = (String) claims.get("given_name");
            if (providerUserId == null) {
                throw new BadRequestException("id_token không hợp lệ (thiếu sub)");
            }
        } else {
            throw new BadRequestException("Hiện chỉ hỗ trợ đăng nhập bằng id_token. Gửi id_token từ Google/Facebook.");
        }
        Optional<UserProvider> upOpt = userProviderRepository.findByProviderAndProviderUserId(provider, providerUserId);
        if (upOpt.isPresent()) {
            User user = upOpt.get().getUser();
            if (!Boolean.TRUE.equals(user.getIsActive())) {
                throw new BadCredentialsException("Tài khoản đã bị khóa");
            }
            return buildAuthResponse(user);
        }
        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null) {
            UserProvider up = UserProvider.builder().user(user).provider(provider).providerUserId(providerUserId).build();
            userProviderRepository.save(up);
            return buildAuthResponse(user);
        }
        Role role = roleRepository.findByName(TRAVELER_ROLE)
                .orElseGet(() -> roleRepository.save(Role.builder().name(TRAVELER_ROLE).description("Khách hàng").build()));
        user = User.builder()
                .email(email != null ? email : providerUserId + "@" + provider + ".oauth")
                .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                .fullName(fullName != null ? fullName : "User")
                .role(role)
                .isActive(true)
                .build();
        user = userRepository.save(user);
        userProviderRepository.save(UserProvider.builder().user(user).provider(provider).providerUserId(providerUserId).build());
        return buildAuthResponse(user);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> decodeIdTokenPayload(String idToken) {
        String[] parts = idToken.split("\\.");
        if (parts.length < 2) throw new BadRequestException("id_token không đúng định dạng");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> map = mapper.readValue(payload, Map.class);
            return map;
        } catch (Exception e) {
            throw new BadRequestException("Không thể đọc id_token");
        }
    }

    /** Luồng 1.7: Đổi mật khẩu (đã đăng nhập). */
    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("Mật khẩu hiện tại không đúng");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        refreshTokenRepository.findByUserAndRevokedFalse(user).forEach(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtProvider.generateAccessToken(
                user.getId().toString(),
                user.getEmail(),
                user.getRole().getName()
        );
        String refreshToken = jwtProvider.generateRefreshToken(user.getId().toString());
        String hash = hashToken(refreshToken);
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(hash)
                .expiresAt(Instant.now().plusMillis(jwtProvider.getJwtProperties().getRefreshTokenValidityMs()))
                .revoked(false)
                .build());
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtProvider.getJwtProperties().getAccessTokenValidityMs() / 1000)
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .fullName(user.getFullName())
                        .role(user.getRole().getName())
                        .avatarUrl(user.getAvatarUrl())
                        .build())
                .build();
    }

    private String hashToken(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(token.getBytes());
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public JwtProvider getJwtProvider() {
        return jwtProvider;
    }
}
