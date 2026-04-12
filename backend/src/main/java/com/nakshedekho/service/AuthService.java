package com.nakshedekho.service;

import jakarta.mail.MessagingException;
import com.nakshedekho.dto.*;
import com.nakshedekho.model.Role;
import com.nakshedekho.model.User;
import com.nakshedekho.repository.UserRepository;
import com.nakshedekho.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HexFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    // CSPRNG — replaces insecure Math.random() for OTP and token generation
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // Password-reset token TTL
    private static final long RESET_TOKEN_TTL_MINUTES = 15;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    @Value("${google.client.id:YOUR_GOOGLE_CLIENT_ID.apps.googleusercontent.com}")
    private String googleClientId;

    // ══════════════════════════════════════════════════════════════════════════
    // GOOGLE LOGIN
    // ══════════════════════════════════════════════════════════════════════════

    public AuthResponse loginWithGoogle(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new RuntimeException("Invalid Google ID Token.");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name  = (String) payload.get("name");

            if (!email.toLowerCase().endsWith("@gmail.com")) {
                throw new RuntimeException("Only @gmail.com addresses are allowed.");
            }

            User user = userRepository.findByEmail(email).orElseGet(() -> {
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setFullName(name);
                newUser.setRole(Role.CUSTOMER);   // Force CUSTOMER — never trust external claim
                newUser.setVerified(true);
                newUser.setActive(true);
                // Placeholder password — CSPRNG-generated, never exposed
                newUser.setPassword(passwordEncoder.encode(generateRandomPassword()));
                return userRepository.save(newUser);
            });

            String token = jwtUtil.generateToken(user);
            return createAuthResponse(user, token);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            logger.warn("Google authentication failed");
            throw new RuntimeException("Google Authentication failed.");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // REGISTER
    // ══════════════════════════════════════════════════════════════════════════

    public String register(RegisterRequest request) {
        if (request.getEmail() == null || !request.getEmail().toLowerCase().endsWith("@gmail.com")) {
            throw new RuntimeException("Invalid Email: Only @gmail.com addresses are allowed.");
        }

        String email = request.getEmail().trim().toLowerCase();
        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent() && existingUser.get().isVerified()) {
            // Generic — never confirm whether an email is registered (enumeration prevention)
            throw new RuntimeException("Registration failed. Please try again or contact support.");
        }

        String otp = generateOtp();

        // Send OTP BEFORE persisting — if email fails, nothing is saved
        try {
            sendOtpEmail(email, otp);
        } catch (Exception e) {
            logger.error("OTP email delivery failed during registration");
            throw new RuntimeException("Registration failed: Could not send verification email. Please try again.");
        }

        User user = existingUser.orElseGet(User::new);
        user.setEmail(email);
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setProfessionalCategory(request.getProfessionalCategory());

        // SECURITY FIX: Always force CUSTOMER role — never trust the role field from the request body.
        // Privileged roles (MANAGER_ADMIN, OWNER_ADMIN) can only be created by an OWNER_ADMIN.
        user.setRole(Role.CUSTOMER);

        // Store OTP as BCrypt hash — never keep plaintext OTP in DB
        user.setOtpCode(passwordEncoder.encode(otp));
        user.setOtpExpiryTime(LocalDateTime.now().plusMinutes(10));
        user.setVerified(false);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());

        userRepository.save(user);
        return "Registration successful. Please check your email for the verification code.";
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SEND OTP (resend)
    // ══════════════════════════════════════════════════════════════════════════

    public void sendOtp(String email) {
        String cleanEmail = email.trim().toLowerCase();

        Optional<User> userOpt = userRepository.findByEmail(cleanEmail);
        if (userOpt.isEmpty()) {
            // Silent return — do NOT confirm whether email exists
            logger.warn("OTP resend requested for unknown email (not disclosed to caller)");
            return;
        }

        User user = userOpt.get();
        String otp = generateOtp();

        user.setOtpCode(passwordEncoder.encode(otp));
        user.setOtpExpiryTime(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        try {
            sendOtpEmail(cleanEmail, otp);
        } catch (MessagingException e) {
            logger.error("OTP resend email failed");
            throw new RuntimeException("Could not resend OTP. Please try again.");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // VERIFY EMAIL
    // ══════════════════════════════════════════════════════════════════════════

    public AuthResponse verifyUser(String email, String otp) {
        final String genericErr = "Invalid or expired verification code.";

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException(genericErr));

        if (user.getOtpCode() == null) {
            throw new RuntimeException(genericErr);
        }

        // Constant-time comparison via BCrypt — prevents timing attacks
        if (!passwordEncoder.matches(otp, user.getOtpCode())) {
            throw new RuntimeException(genericErr);
        }

        if (user.getOtpExpiryTime() == null || user.getOtpExpiryTime().isBefore(LocalDateTime.now())) {
            user.setOtpCode(null);
            user.setOtpExpiryTime(null);
            userRepository.save(user);
            throw new RuntimeException("Verification code has expired. Please request a new one.");
        }

        user.setVerified(true);
        user.setOtpCode(null);
        user.setOtpExpiryTime(null);
        userRepository.save(user);

        String token = jwtUtil.generateToken(user);
        return createAuthResponse(user, token);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // LOGIN (with brute-force lockout tracking)
    // ══════════════════════════════════════════════════════════════════════════

    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword()));

            User user = (User) authentication.getPrincipal();

            // Email must be verified before login is permitted
            if (!user.isVerified()) {
                throw new RuntimeException("Email not verified. Please verify your email before logging in.");
            }

            // Successful login — reset lockout counter
            user.recordSuccessfulLogin();
            userRepository.save(user);

            String token = jwtUtil.generateToken(user);
            return createAuthResponse(user, token);

        } catch (LockedException e) {
            throw new RuntimeException("Account temporarily locked due to too many failed attempts. Please try again in 15 minutes.");
        } catch (DisabledException e) {
            throw new RuntimeException("Account is disabled. Please contact support.");
        } catch (BadCredentialsException e) {
            // Increment failed-login counter for this user
            userRepository.findByEmail(email).ifPresent(u -> {
                u.recordFailedLogin();
                userRepository.save(u);
                logger.warn("Failed login attempt {}/{} for account {}",
                        u.getFailedLoginAttempts(), 5, email);
            });
            // Generic error — never distinguish "wrong password" vs "no such user"
            throw new RuntimeException("Invalid credentials.");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // FORGOT PASSWORD — send reset token by email
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Generates a cryptographically random 64-char reset token, hashes it with SHA-256,
     * stores the HASH in the DB, and emails the PLAINTEXT token to the user.
     *
     * Token expires in 15 minutes and is single-use (cleared after reset-password succeeds).
     */
    public void forgotPassword(String email) {
        String cleanEmail = email.trim().toLowerCase();

        // Always return silently — never confirm whether the email is registered
        Optional<User> userOpt = userRepository.findByEmail(cleanEmail);
        if (userOpt.isEmpty()) {
            logger.warn("Password reset requested for unknown email (not disclosed to caller)");
            return;
        }

        User user = userOpt.get();

        // Generate 64-char URL-safe token with SecureRandom
        byte[] tokenBytes = new byte[48];
        SECURE_RANDOM.nextBytes(tokenBytes);
        String plainToken = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(tokenBytes);

        // Store SHA-256 hash — never the plaintext token
        user.setResetToken(sha256hex(plainToken));
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(RESET_TOKEN_TTL_MINUTES));
        userRepository.save(user);

        try {
            sendPasswordResetEmail(cleanEmail, plainToken);
        } catch (Exception e) {
            logger.error("Password reset email failed to send");
            // Clear the token so a stale hash isn't left in DB
            user.setResetToken(null);
            user.setResetTokenExpiry(null);
            userRepository.save(user);
            throw new RuntimeException("Could not send reset email. Please try again.");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // RESET PASSWORD — validate token and set new password
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Validates the reset token (by comparing SHA-256 hash), checks expiry,
     * enforces password strength, then updates the password and invalidates the token.
     */
    public void resetPassword(String token, String newPassword) {
        final String genericErr = "Invalid or expired password reset link.";

        if (token == null || token.isBlank()) {
            throw new RuntimeException(genericErr);
        }

        String tokenHash = sha256hex(token);

        User user = userRepository.findByResetToken(tokenHash)
                .orElseThrow(() -> new RuntimeException(genericErr));

        // Check expiry
        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            // Clear expired token
            user.setResetToken(null);
            user.setResetTokenExpiry(null);
            userRepository.save(user);
            throw new RuntimeException("Password reset link has expired. Please request a new one.");
        }

        // Enforce same password strength rules as registration
        validatePasswordStrength(newPassword);

        // All checks passed — update password and invalidate token (one-time use)
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        user.recordSuccessfulLogin();  // Also clear any lockout state
        userRepository.save(user);

        logger.info("Password reset completed for user {}", user.getId());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private void sendOtpEmail(String email, String otp) throws MessagingException {
        String subject = "Verify your NaksheDekho Account";
        String body = "<html><body style='font-family:Arial,sans-serif;padding:20px;'>"
                + "<div style='max-width:600px;margin:auto;border:1px solid #e2e8f0;border-radius:10px;overflow:hidden;'>"
                + "<div style='background:#0d3b66;padding:20px;text-align:center;color:white;'>"
                + "<h2>NaksheDekho Verification</h2></div>"
                + "<div style='padding:30px;text-align:center;'>"
                + "<p style='font-size:16px;'>Use this code to verify your account:</p>"
                + "<h1 style='color:#0d3b66;font-size:48px;letter-spacing:10px;margin:20px 0;'>" + otp + "</h1>"
                + "<p style='color:#64748b;font-size:14px;'>Expires in <strong>10 minutes</strong>. Do not share.</p>"
                + "</div></div></body></html>";
        emailService.sendEmail(email, subject, body);
    }

    private void sendPasswordResetEmail(String email, String token) throws MessagingException {
        // In production this should be your real domain URL
        String resetLink = "http://localhost:8080/reset-password.html?token=" + token;
        String subject = "Reset your NaksheDekho Password";
        String body = "<html><body style='font-family:Arial,sans-serif;padding:20px;'>"
                + "<div style='max-width:600px;margin:auto;border:1px solid #e2e8f0;border-radius:10px;overflow:hidden;'>"
                + "<div style='background:#0d3b66;padding:20px;text-align:center;color:white;'>"
                + "<h2>Password Reset</h2></div>"
                + "<div style='padding:30px;text-align:center;'>"
                + "<p>You requested a password reset for your NaksheDekho account.</p>"
                + "<a href='" + resetLink + "' style='display:inline-block;margin:20px 0;padding:12px 30px;"
                + "background:#0d3b66;color:white;text-decoration:none;border-radius:6px;font-size:16px;'>Reset Password</a>"
                + "<p style='color:#64748b;font-size:13px;'>This link expires in <strong>15 minutes</strong> and can only be used once.</p>"
                + "<p style='color:#64748b;font-size:13px;'>If you did not request this, please ignore this email.</p>"
                + "</div></div></body></html>";
        emailService.sendEmail(email, subject, body);
    }

    /** 6-digit OTP using CSPRNG — never Math.random(). */
    private String generateOtp() {
        int otp = 100000 + SECURE_RANDOM.nextInt(900000);
        return String.valueOf(otp);
    }

    /** High-entropy random password for Google sign-in placeholder accounts. */
    private String generateRandomPassword() {
        byte[] bytes = new byte[24];
        SECURE_RANDOM.nextBytes(bytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 hex digest — used to store reset tokens securely. */
    private String sha256hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }

    /**
     * Validates password strength — same rules as RegisterRequest @Pattern annotation.
     * Applied to reset-password so the strength policy is consistently enforced.
     */
    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new RuntimeException("Password must be at least 8 characters.");
        }
        if (!password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$")) {
            throw new RuntimeException(
                    "Password must contain uppercase, lowercase, a digit, and a special character (@$!%*?&).");
        }
    }

    private AuthResponse createAuthResponse(User user, String token) {
        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setRole(user.getRole().name());
        response.setUserId(user.getId());

        boolean hasActive = user.hasActiveSubscription();
        response.setHasActiveSubscription(hasActive);

        if (hasActive && user.getActiveSubscription() != null) {
            response.setSubscriptionTier(user.getActiveSubscription().getPackageTier());
            response.setServiceDiscountPercentage(user.getActiveSubscription().getServiceDiscountPercentage());
        } else {
            response.setServiceDiscountPercentage(0);
        }

        return response;
    }
}
