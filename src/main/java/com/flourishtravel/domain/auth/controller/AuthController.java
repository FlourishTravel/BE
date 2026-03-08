package com.flourishtravel.domain.auth.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.domain.auth.dto.AuthResponse;
import com.flourishtravel.domain.auth.dto.LoginRequest;
import com.flourishtravel.domain.auth.dto.RegisterRequest;
import com.flourishtravel.domain.auth.service.AuthService;
import com.flourishtravel.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse res = authService.register(request);
        return ResponseEntity.ok(ApiResponse.ok("Đăng ký thành công", res));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse res = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok(res));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Thiếu refreshToken"));
        }
        AuthResponse res = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(ApiResponse.ok(res));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody(required = false) Map<String, String> body) {
        String refreshToken = body != null ? body.get("refreshToken") : null;
        authService.logout(refreshToken);
        return ResponseEntity.ok(ApiResponse.ok("Đã đăng xuất", null));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Email không được để trống"));
        }
        authService.forgotPassword(email.trim());
        return ResponseEntity.ok(ApiResponse.ok("Nếu email tồn tại, bạn sẽ nhận link đặt lại mật khẩu", null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        String newPassword = body.get("new_password");
        if (token == null || token.isBlank() || newPassword == null || newPassword.length() < 6) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Token và mật khẩu mới (tối thiểu 6 ký tự) là bắt buộc"));
        }
        authService.resetPassword(token, newPassword);
        return ResponseEntity.ok(ApiResponse.ok("Đặt lại mật khẩu thành công", null));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, String> body) {
        if (principal == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập"));
        }
        String current = body.get("current_password");
        String newPass = body.get("new_password");
        if (current == null || newPass == null || newPass.length() < 6) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Mật khẩu hiện tại và mật khẩu mới (tối thiểu 6 ký tự) là bắt buộc"));
        }
        authService.changePassword(principal.getId(), current, newPass);
        return ResponseEntity.ok(ApiResponse.ok("Đổi mật khẩu thành công", null));
    }

    /** Luồng 1.3: Đăng nhập OAuth (chung). Body: { provider: "google"|"facebook", id_token } */
    @PostMapping("/oauth")
    public ResponseEntity<ApiResponse<AuthResponse>> oauth(@RequestBody Map<String, String> body) {
        String provider = body.get("provider");
        String idToken = body.get("id_token");
        String code = body.get("code");
        if (provider == null || provider.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("provider là bắt buộc"));
        }
        if ((idToken == null || idToken.isBlank()) && (code == null || code.isBlank())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Cần id_token hoặc code"));
        }
        AuthResponse res = authService.oauthLogin(provider.trim().toLowerCase(), idToken != null ? idToken.trim() : null, code != null ? code.trim() : null);
        return ResponseEntity.ok(ApiResponse.ok(res));
    }

    /** Đăng nhập bằng Google – body: { "id_token": "<id_token từ Google Sign-In>" } */
    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthResponse>> google(@RequestBody Map<String, String> body) {
        String idToken = body != null ? body.get("id_token") : null;
        if (idToken == null || idToken.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("id_token là bắt buộc"));
        }
        AuthResponse res = authService.oauthLogin("google", idToken.trim(), null);
        return ResponseEntity.ok(ApiResponse.ok(res));
    }

    /** Đăng nhập bằng Facebook – body: { "id_token": "<id_token từ Facebook Login>" } */
    @PostMapping("/facebook")
    public ResponseEntity<ApiResponse<AuthResponse>> facebook(@RequestBody Map<String, String> body) {
        String idToken = body != null ? body.get("id_token") : null;
        if (idToken == null || idToken.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("id_token là bắt buộc"));
        }
        AuthResponse res = authService.oauthLogin("facebook", idToken.trim(), null);
        return ResponseEntity.ok(ApiResponse.ok(res));
    }
}
