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

@Component
@RequiredArgsConstructor
public class UserSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(UserSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.owner.email:owner@nakshedekho.com}")
    private String ownerEmail;

    @Value("${admin.owner.password:Admin@123}")
    private String ownerPassword;

    @Value("${admin.owner2.email:kadamuday2003@gmail.com}")
    private String owner2Email;

    @Value("${admin.owner2.password:admin123}")
    private String owner2Password;

    @Value("${admin.manager.email:manager@nakshedekho.com}")
    private String managerEmail;

    @Value("${admin.manager.password:Manager@123}")
    private String managerPassword;

    @Override
    public void run(String... args) throws Exception {
        seedOwner(ownerEmail, "Platform Owner", ownerPassword);
        seedOwner(owner2Email, "Uday Kadam", owner2Password);
        seedManager(managerEmail, "Default Manager", managerPassword);
    }

    private void seedOwner(String email, String fullName, String password) {
        if (!userRepository.existsByEmail(email)) {
            User user = new User();
            user.setEmail(email);
            user.setFullName(fullName);
            user.setPassword(passwordEncoder.encode(password));
            user.setRole(Role.OWNER_ADMIN);
            user.setActive(true);
            user.setVerified(true);
            user.setCreatedAt(LocalDateTime.now());
            userRepository.save(user);
            // Only log email — NEVER log passwords
            logger.info("Admin account created: {}", email);
        } else {
            logger.debug("Admin account already exists: {}", email);
        }
    }

    private void seedManager(String email, String fullName, String password) {
        if (!userRepository.existsByEmail(email)) {
            User user = new User();
            user.setEmail(email);
            user.setFullName(fullName);
            user.setPassword(passwordEncoder.encode(password));
            user.setRole(Role.MANAGER_ADMIN);
            user.setActive(true);
            user.setVerified(true);
            user.setCreatedAt(LocalDateTime.now());
            userRepository.save(user);
            logger.info("Manager account created: {}", email);
        } else {
            logger.debug("Manager account already exists: {}", email);
        }
    }
}
