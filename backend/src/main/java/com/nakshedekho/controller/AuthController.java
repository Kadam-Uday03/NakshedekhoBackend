package com.nakshedekho.controller;

import com.nakshedekho.dto.*;
import com.nakshedekho.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            String message = authService.register(request);
            return ResponseEntity.ok(Map.of("message", message));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> request) {
        try {
            authService.sendOtp(request.get("email"));
            // Always return 200 — do not confirm whether email exists
            return ResponseEntity.ok(Map.of("message", "If an account exists, a verification code has been sent."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@RequestBody Map<String, String> request) {
        try {
            AuthResponse response = authService.verifyUser(
                    request.get("email"), request.get("otp"));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Return 401 with generic message — no stack traces, no field details
            return ResponseEntity.status(401).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> request) {
        try {
            AuthResponse response = authService.loginWithGoogle(request.get("idToken"));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * POST /api/auth/forgot-password
     * Sends a password-reset email with a time-limited token.
     * Always returns 200 — never confirms whether the email is registered.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        try {
            authService.forgotPassword(request.get("email"));
        } catch (Exception e) {
            // Swallow most errors — we don't want to leak info about email existence
        }
        return ResponseEntity.ok(Map.of(
                "message", "If your email is registered, you will receive a reset link shortly."));
    }

    /**
     * POST /api/auth/reset-password
     * Body: { "token": "...", "newPassword": "..." }
     * Validates the token hash, checks expiry, applies new password.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        try {
            authService.resetPassword(request.get("token"), request.get("newPassword"));
            return ResponseEntity.ok(Map.of("message", "Password has been reset successfully. Please log in."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
