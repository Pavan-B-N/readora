package com.readora.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** A hashed, individually revocable refresh token issued to one user. */
@Entity
@Table(name = "refresh_tokens", schema = "auth")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "ip_address")
    private String ipAddress;

    /** No-arg constructor required by JPA; not for application use. */
    protected RefreshToken() {
    }

    /** tokenHash must be the SHA-256 hash of the raw token — the raw value is never stored. */
    public RefreshToken(User user, String tokenHash, Instant expiresAt, String userAgent, String ipAddress) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.userAgent = userAgent;
        this.ipAddress = ipAddress;
    }

    // Primary key.
    public UUID getId() {
        return id;
    }

    // Owning user.
    public User getUser() {
        return user;
    }

    // Hash of the raw token value.
    public String getTokenHash() {
        return tokenHash;
    }

    // When this token stops being valid.
    public Instant getExpiresAt() {
        return expiresAt;
    }

    // When this token was revoked, or null if still active.
    public Instant getRevokedAt() {
        return revokedAt;
    }

    // User-agent string captured when the token was issued.
    public String getUserAgent() {
        return userAgent;
    }

    // IP address captured when the token was issued.
    public String getIpAddress() {
        return ipAddress;
    }

    // Whether this token has been revoked.
    public boolean isRevoked() {
        return revokedAt != null;
    }

    // Whether this token's expiry has passed.
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /** Idempotent if called more than once. */
    public void revoke() {
        this.revokedAt = Instant.now();
    }

    /** Id-based equality, and only for persisted entities. */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof RefreshToken)) {
            return false;
        }

        RefreshToken that = (RefreshToken) obj;

        return id != null && Objects.equals(id, that.id);
    }

    // Id-based hash code, consistent with equals().
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
