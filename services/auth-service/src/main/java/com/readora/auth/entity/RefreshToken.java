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

    /**
     * Creates a new, unrevoked refresh token.
     *
     * @param user      the user this token belongs to
     * @param tokenHash the SHA-256 hash of the raw token value — the raw value is never stored
     * @param expiresAt the instant after which this token is no longer valid
     * @param userAgent the User-Agent header of the client that requested the token, or null
     * @param ipAddress the IP address of the client that requested the token, or null
     */
    public RefreshToken(User user, String tokenHash, Instant expiresAt, String userAgent, String ipAddress) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.userAgent = userAgent;
        this.ipAddress = ipAddress;
    }

    /** @return the token's primary key */
    public UUID getId() {
        return id;
    }

    /** @return the user this token belongs to */
    public User getUser() {
        return user;
    }

    /** @return the SHA-256 hash of the raw token value */
    public String getTokenHash() {
        return tokenHash;
    }

    /** @return the instant after which this token is no longer valid */
    public Instant getExpiresAt() {
        return expiresAt;
    }

    /** @return the instant this token was revoked, or null if it has not been revoked */
    public Instant getRevokedAt() {
        return revokedAt;
    }

    /** @return the User-Agent header captured when this token was issued, or null */
    public String getUserAgent() {
        return userAgent;
    }

    /** @return the IP address captured when this token was issued, or null */
    public String getIpAddress() {
        return ipAddress;
    }

    /** @return true if this token has been revoked (revokedAt is set) */
    public boolean isRevoked() {
        return revokedAt != null;
    }

    /** @return true if the current time is after this token's expiresAt */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /** Marks this token revoked as of now. Idempotent if called more than once. */
    public void revoke() {
        this.revokedAt = Instant.now();
    }

    /**
     * Equality is id-based only, and only for persisted entities.
     *
     * @param obj the object to compare against
     * @return true if obj is a RefreshToken with the same non-null id
     */
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

    /** @return a hash code consistent with the id-based equals() implementation */
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
