package com.nakshedekho.service;

import jakarta.mail.MessagingException;
import com.nakshedekho.dto.*;
import com.nakshedekho.model.User;
import com.nakshedekho.repository.UserRepository;
import com.nakshedekho.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.nakshedekho.model.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    private final EmailService emailService;

    @Value("${google.client.id:YOUR_GOOGLE_CLIENT_ID.apps.googleusercontent.com}")
    private String googleClientId;

    public AuthResponse loginWithGoogle(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(),
                    new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new RuntimeException("Invalid Google ID Token");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");

            if (!email.toLowerCase().endsWith("@gmail.com")) {
                throw new RuntimeException("Only @gmail.com addresses are allowed.");
            }

            Optional<User> userOptional = userRepository.findByEmail(email);
            User user;

            if (userOptional.isPresent()) {
                user = userOptional.get();
            } else {
                // Create new user if not exists
                user = new User();
                user.setEmail(email);
                user.setFullName(name);
                user.setRole(Role.CUSTOMER);
                user.setVerified(true);
                user.setActive(true);
                // Set a default password that they can change later
                // In a real app, we might force them to change it on first direct login
                user.setPassword(passwordEncoder.encode("GoogleLogin@123"));
                userRepository.save(user);
            }

            String token = jwtUtil.generateToken(user);
            return createAuthResponse(user, token);

        } catch (Exception e) {
            throw new RuntimeException("Google Authentication failed: " + e.getMessage());
        }
    }

    public String register(RegisterRequest request) {
        System.out.println("DEBUG: Starting registration for email: " + request.getEmail());

        // 1. Standard Validation
        if (request.getEmail() == null || !request.getEmail().toLowerCase().endsWith("@gmail.com")) {
            throw new RuntimeException("Invalid Email: Only @gmail.com addresses are allowed.");
        }

        String email = request.getEmail().trim().toLowerCase();
        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent() && existingUser.get().isVerified()) {
            throw new RuntimeException("Email already exists and is verified.");
        }

        // 2. Generate OTP first (as requested)
        String otp = generateOtp();

        // 3. Try to send OTP before saving user to DB (as requested)
        boolean mailSent = false;
        try {
            sendOtpEmail(email, otp);
            mailSent = true;
            System.out.println("DEBUG: OTP sent successfully to " + email);
        } catch (Exception e) {
            System.err.println("CRITICAL: OTP Email delivery failed: " + e.getMessage());
            throw new RuntimeException("Registration failed: Could not send verification email. " + e.getMessage());
        }

        // 4. If mailSent is true, then save/update user in DB
        if (mailSent) {
            User user;
            if (existingUser.isPresent()) {
                user = existingUser.get();
                System.out.println("DEBUG: Updating existing unverified user: " + email);
            } else {
                user = new User();
                user.setEmail(email);
                System.out.println("DEBUG: Creating new user entry: " + email);
            }

            // Set user details
            user.setFullName(request.getFullName());
            user.setPhone(request.getPhone());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setProfessionalCategory(request.getProfessionalCategory());
            user.setRole(request.getRole());

            // Set OTP info
            user.setOtpCode(otp);
            user.setOtpExpiryTime(java.time.LocalDateTime.now().plusMinutes(5));
            user.setVerified(false);

            // Explicitly set active status to avoid MySQL "default value" error
            user.setActive(true);
            user.setCreatedAt(java.time.LocalDateTime.now());

            userRepository.save(user);
            return "Registration successful. Please check your email for OTP.";
        } else {
            throw new RuntimeException("Internal Error: Mail was not sent correctly.");
        }
    }

    public void sendOtp(String email) {
        System.out.println("DEBUG: Sending OTP for email: " + email);
        String cleanEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new RuntimeException("BACKEND ERROR: USER NOT FOUND: " + cleanEmail));

        String otp = generateOtp();
        user.setOtpCode(otp);
        user.setOtpExpiryTime(java.time.LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        try {
            sendOtpEmail(cleanEmail, otp);
        } catch (MessagingException e) {
            throw new RuntimeException("Resending OTP failed: " + e.getMessage());
        }
    }

    private void sendOtpEmail(String email, String otp) throws MessagingException {
        try {
            String subject = "Verify your NaksheDekho Account";
            String body = "<html>" +
                    "<body style='font-family: Arial, sans-serif; padding: 20px;'>" +
                    "<div style='max-width: 600px; margin: auto; border: 1px solid #e2e8f0; border-radius: 10px; overflow: hidden;'>"
                    +
                    "<div style='background: #0d3b66; padding: 20px; text-align: center; color: white;'>" +
                    "<h2>NaksheDekho Verification</h2>" +
                    "</div>" +
                    "<div style='padding: 30px; text-align: center;'>" +
                    "<p style='font-size: 16px;'>Welcome to NaksheDekho! Use the code below to verify your account:</p>"
                    +
                    "<h1 style='color: #0d3b66; font-size: 48px; letter-spacing: 10px; margin: 20px 0;'>" + otp
                    + "</h1>" +
                    "<p style='color: #64748b; font-size: 14px;'>This code will expire in 5 minutes.</p>" +
                    "</div>" +
                    "</div>" +
                    "</body>" +
                    "</html>";

            emailService.sendEmail(email, subject, body);
        } catch (MessagingException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to prepare OTP email: " + e.getMessage());
        }
    }

    public AuthResponse verifyUser(String email, String otp) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("VERIFY_USER_ERROR: User not found for email: " + email));

        if (user.getOtpCode() == null || !user.getOtpCode().equals(otp)) {
            throw new RuntimeException("Invalid OTP");
        }

        if (user.getOtpExpiryTime().isBefore(java.time.LocalDateTime.now())) {
            throw new RuntimeException("OTP has expired");
        }

        user.setVerified(true);
        user.setOtpCode(null);
        user.setOtpExpiryTime(null);
        userRepository.save(user);

        String token = jwtUtil.generateToken(user);
        return createAuthResponse(user, token);
    }

    private String generateOtp() {
        return String.valueOf(100000 + (int) (Math.random() * 900000));
    }

    // OTP related methods removed as per request to remove previous
    // login/verification flow

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = (User) authentication.getPrincipal();
        String token = jwtUtil.generateToken(user);

        return createAuthResponse(user, token);
    }

    private AuthResponse createAuthResponse(User user, String token) {
        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setRole(user.getRole().name());
        response.setUserId(user.getId());

        // Add subscription info
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
