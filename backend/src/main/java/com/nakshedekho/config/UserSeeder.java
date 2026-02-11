package com.nakshedekho.config;

import com.nakshedekho.model.Role;
import com.nakshedekho.model.User;
import com.nakshedekho.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class UserSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Create Owner if not exists
        String ownerEmail = "owner@nakshedekho.com";
        User owner = userRepository.findByEmail(ownerEmail).orElse(null);

        if (owner == null) {
            owner = new User();
            owner.setEmail(ownerEmail);
            owner.setFullName("Platform Owner");
            owner.setPassword(passwordEncoder.encode("Admin@123"));
            owner.setRole(Role.OWNER_ADMIN);
            owner.setActive(true);
            owner.setVerified(true);
            owner.setCreatedAt(LocalDateTime.now());
            userRepository.save(owner);
            System.out.println("✅ Default owner account created: " + ownerEmail + " / Admin@123");
        } else {
            // Ensure owner is active and verified, and reset password to be sure
            owner.setActive(true);
            owner.setVerified(true);
            owner.setRole(Role.OWNER_ADMIN); // Ensure correct role
            owner.setPassword(passwordEncoder.encode("Admin@123"));
            userRepository.save(owner);
            System.out.println("✅ Default owner account verified and password reset: " + ownerEmail + " / Admin@123");
        }

        // Also ensure owner@test.com is usable if it exists
        String testOwnerEmail = "owner@test.com";
        User testOwner = userRepository.findByEmail(testOwnerEmail).orElse(null);
        if (testOwner != null) {
            testOwner.setActive(true);
            testOwner.setVerified(true);
            testOwner.setPassword(passwordEncoder.encode("Admin@123"));
            userRepository.save(testOwner);
            System.out.println("✅ Test owner account verified and password reset: " + testOwnerEmail + " / Admin@123");
        }

        // Add requested owner account: kadamuday2003@gmail.com / admin123
        String requestedOwnerEmail = "kadamuday2003@gmail.com";
        User requestedOwner = userRepository.findByEmail(requestedOwnerEmail).orElse(null);
        if (requestedOwner == null) {
            requestedOwner = new User();
            requestedOwner.setEmail(requestedOwnerEmail);
            requestedOwner.setFullName("Uday Kadam");
            requestedOwner.setPassword(passwordEncoder.encode("admin123"));
            requestedOwner.setRole(Role.OWNER_ADMIN);
            requestedOwner.setActive(true);
            requestedOwner.setVerified(true);
            requestedOwner.setCreatedAt(LocalDateTime.now());
            userRepository.save(requestedOwner);
            System.out.println("✅ Requested owner account created: " + requestedOwnerEmail + " / admin123");
        } else {
            // Convert to OWNER_ADMIN if already exists (as CUSTOMER etc)
            requestedOwner.setRole(Role.OWNER_ADMIN);
            requestedOwner.setPassword(passwordEncoder.encode("admin123"));
            requestedOwner.setActive(true);
            requestedOwner.setVerified(true);
            userRepository.save(requestedOwner);
            System.out.println(
                    "✅ Requested owner account updated to OWNER_ADMIN: " + requestedOwnerEmail + " / admin123");
        }

        // Create a test manager if not exists
        String managerEmail = "manager@nakshedekho.com";
        if (!userRepository.existsByEmail(managerEmail)) {
            User manager = new User();
            manager.setEmail(managerEmail);
            manager.setFullName("Default Manager");
            manager.setPassword(passwordEncoder.encode("Manager@123"));
            manager.setRole(Role.MANAGER_ADMIN);
            manager.setActive(true);
            manager.setVerified(true);
            manager.setCreatedAt(LocalDateTime.now());
            userRepository.save(manager);
            System.out.println("✅ Default manager account created: " + managerEmail + " / Manager@123");
        }
    }
}
