package com.nakshedekho.config;

import com.nakshedekho.model.Role;
import com.nakshedekho.model.User;
import com.nakshedekho.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Seeds the initial admin accounts on first boot.
 *
 * Security notes:
 * <ul>
 *   <li>All credentials are read exclusively from environment variables — no fallback defaults.</li>
 *   <li>If a required env var is missing the application will fail to start (Spring throws).</li>
 *   <li>Passwords are BCrypt-encoded before persistence — never stored in plaintext.</li>
 *   <li>Only the email address is logged, never any password.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class UserSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(UserSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // No default fallback values — missing env var causes startup failure (intentional).
    @Value("${admin.owner.email}")
    private String ownerEmail;

    @Value("${admin.owner.password}")
    private String ownerPassword;

    @Value("${admin.owner2.email}")
    private String owner2Email;

    @Value("${admin.owner2.password}")
    private String owner2Password;

    @Value("${admin.manager.email}")
    private String managerEmail;

    @Value("${admin.manager.password}")
    private String managerPassword;

    @Override
    public void run(String... args) {
        seedOwner(ownerEmail, "Platform Owner", ownerPassword);
        seedOwner(owner2Email, "Uday Kadam", owner2Password);
        seedManager(managerEmail, "Default Manager", managerPassword);
    }

    private void seedOwner(String email, String fullName, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return;
        }
        upsertUser(email, fullName, password, Role.OWNER_ADMIN);
    }

    private void seedManager(String email, String fullName, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return;
        }
        upsertUser(email, fullName, password, Role.MANAGER_ADMIN);
    }

    private void upsertUser(String email, String fullName, String password, Role role) {
        userRepository.findByEmail(email).ifPresentOrElse(
            existing -> {
                // Update password if env gets changed, useful for local testing and recovery
                existing.setPassword(passwordEncoder.encode(password));
                existing.setRole(role);
                userRepository.save(existing);
                logger.info("Admin ({}) account updated via .env: {}", role, email);
            },
            () -> {
                User user = new User();
                user.setEmail(email);
                user.setFullName(fullName);
                user.setPassword(passwordEncoder.encode(password));
                user.setRole(role);
                user.setActive(true);
                user.setVerified(true);
                user.setCreatedAt(LocalDateTime.now());
                userRepository.save(user);
                logger.info("Admin ({}) account seeded via .env: {}", role, email);
            }
        );
    }
}
