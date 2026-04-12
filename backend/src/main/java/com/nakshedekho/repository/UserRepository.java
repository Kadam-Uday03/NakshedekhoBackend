package com.nakshedekho.repository;

import com.nakshedekho.model.Role;
import com.nakshedekho.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Boolean existsByEmail(String email);

    List<User> findByRole(Role role);

    List<User> findByRoleAndActiveTrue(Role role);

    List<User> findByActiveSubscription_Id(Long packageId);

    /** Used by the password-reset flow — looks up by SHA-256 hash of the emailed token. */
    Optional<User> findByResetToken(String resetTokenHash);
}

