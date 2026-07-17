package com.readora.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * A registered account's credentials. Credentials only — no display name, avatar, or other
 * profile data. That lives in user-service.
 */
@Entity
@Table(name = "users", schema = "auth")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            schema = "auth",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    /** No-arg constructor required by JPA; not for application use. */
    protected User() {
    }

    /** passwordHash must already be a BCrypt hash — never a plaintext password. */
    public User(String email, String passwordHash) {
        this.email = email;
        this.passwordHash = passwordHash;
    }

    /** JPA lifecycle callback: stamps createdAt/updatedAt immediately before the first INSERT. */
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** JPA lifecycle callback: refreshes updatedAt immediately before every subsequent UPDATE. */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Primary key.
    public UUID getId() {
        return id;
    }

    // Account email address.
    public String getEmail() {
        return email;
    }

    // Updates the account email address.
    public void setEmail(String email) {
        this.email = email;
    }

    // BCrypt hash of the account password.
    public String getPasswordHash() {
        return passwordHash;
    }

    // Updates the stored password hash.
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    // Current account status.
    public UserStatus getStatus() {
        return status;
    }

    // Updates the account status.
    public void setStatus(UserStatus status) {
        this.status = status;
    }

    // Whether the account's email has been verified.
    public boolean isEmailVerified() {
        return emailVerified;
    }

    // Updates the email-verified flag.
    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    // Count of consecutive failed login attempts.
    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    // Increments the failed-login counter by one.
    public void incrementFailedLoginAttempts() {
        this.failedLoginAttempts++;
    }

    // Resets the failed-login counter after a successful login.
    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
    }

    // Timestamp of the account's last successful login.
    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    // Updates the last-login timestamp.
    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    // When the account was created.
    public Instant getCreatedAt() {
        return createdAt;
    }

    // When the account was last updated.
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // Roles assigned to this account.
    public Set<Role> getRoles() {
        return roles;
    }

    // Assigns a role to this account.
    public void addRole(Role role) {
        this.roles.add(role);
    }

    // Removes a role from this account.
    public void removeRole(Role role) {
        this.roles.remove(role);
    }

    /** Id-based equality, and only for persisted entities — two transient users are never equal. */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof User)) {
            return false;
        }

        User user = (User) obj;

        return Objects.equals(id, user.id);
    }

    // Id-based hash code, consistent with equals().
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
