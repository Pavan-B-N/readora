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

    /**
     * Creates a new user with default status/verification/lockout state.
     *
     * @param email        the account's unique email address
     * @param passwordHash the BCrypt hash of the account's password — never a plaintext password
     */
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

    /** @return the user's primary key */
    public UUID getId() {
        return id;
    }

    /** @return the user's email address */
    public String getEmail() {
        return email;
    }

    /** @param email the new email address to set */
    public void setEmail(String email) {
        this.email = email;
    }

    /** @return the BCrypt hash of the user's password */
    public String getPasswordHash() {
        return passwordHash;
    }

    /** @param passwordHash the new BCrypt password hash to set */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /** @return the account's current status (ACTIVE, LOCKED, or DISABLED) */
    public UserStatus getStatus() {
        return status;
    }

    /** @param status the new account status to set */
    public void setStatus(UserStatus status) {
        this.status = status;
    }

    /** @return true if the account's email address has been verified */
    public boolean isEmailVerified() {
        return emailVerified;
    }

    /** @param emailVerified the new email-verified flag to set */
    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    /** @return the number of consecutive failed login attempts since the last successful login */
    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    /** Increments the consecutive-failed-login counter by one, called after a wrong password. */
    public void incrementFailedLoginAttempts() {
        this.failedLoginAttempts++;
    }

    /** Resets the consecutive-failed-login counter to zero, called after a successful login. */
    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
    }

    /** @return the timestamp of the user's last successful login, or null if never logged in */
    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    /** @param lastLoginAt the new last-successful-login timestamp to set */
    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    /** @return the timestamp the account was created */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /** @return the timestamp the account was last modified */
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /** @return the set of roles currently assigned to this user */
    public Set<Role> getRoles() {
        return roles;
    }

    /** @param role the role to assign to this user */
    public void addRole(Role role) {
        this.roles.add(role);
    }

    /** @param role the role to remove from this user */
    public void removeRole(Role role) {
        this.roles.remove(role);
    }

    /**
     * Equality is id-based only, and only for persisted entities — two transient (unsaved)
     * users are never considered equal to each other even if otherwise identical.
     *
     * @param obj the object to compare against
     * @return true if obj is a User with the same non-null id
     */
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

    /** @return a hash code consistent with the id-based equals() implementation */
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
