package com.nakshedekho.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_reset_token", columnList = "resetToken"),
    @Index(name = "idx_email", columnList = "email")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<InteriorProject> projects;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore   // Never serialise password hash to JSON responses
    private String password;

    @Column(nullable = false)
    private String fullName;

    private String phone;

    private String professionalCategory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private Boolean active;

    @ManyToOne
    @JoinColumn(name = "active_subscription_id")
    private DesignPackage activeSubscription;

    private LocalDateTime subscriptionStartDate;
    private LocalDateTime subscriptionEndDate;

    @Column(name = "projects_used_under_subscription")
    private Integer projectsUsedUnderSubscription = 0;

    // ── OTP Verification ──────────────────────────────────────────────────────
    @Column(columnDefinition = "boolean default false")
    private boolean isVerified = false;

    @com.fasterxml.jackson.annotation.JsonIgnore
    private String otpCode;
    private LocalDateTime otpExpiryTime;

    // ── Password Reset (token hash stored, never plaintext) ───────────────────
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String resetToken;       // SHA-256 hex of the emailed token — never the token itself
    private LocalDateTime resetTokenExpiry;

    // ── Account Lockout (brute-force protection) ──────────────────────────────
    @Column(columnDefinition = "int default 0")
    private int failedLoginAttempts = 0;
    private LocalDateTime lockedUntil;  // null = not locked

    private static final int  MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_MINUTES        = 15;

    /** Called after each failed password check — increments counter and locks if threshold hit. */
    public void recordFailedLogin() {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= MAX_FAILED_ATTEMPTS) {
            this.lockedUntil = LocalDateTime.now().plusMinutes(LOCK_MINUTES);
        }
    }

    /** Called on successful authentication — clears lockout state. */
    public void recordSuccessfulLogin() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }

    // ── Subscription helpers ──────────────────────────────────────────────────
    public boolean hasActiveSubscription() {
        if (activeSubscription == null || subscriptionEndDate == null) return false;
        return LocalDateTime.now().isBefore(subscriptionEndDate);
    }

    // ── JPA lifecycle ─────────────────────────────────────────────────────────
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (active == null)    active = true;
    }

    // ── UserDetails implementation ────────────────────────────────────────────
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override public String  getUsername()              { return email; }
    @Override public boolean isAccountNonExpired()      { return true; }

    /**
     * Returns false if the account is currently within its lockout window.
     * Spring Security maps this to LockedException → 401.
     */
    @Override
    public boolean isAccountNonLocked() {
        if (lockedUntil == null) return true;
        return LocalDateTime.now().isAfter(lockedUntil);  // auto-unlock when window passes
    }

    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return Boolean.TRUE.equals(active); }
}
