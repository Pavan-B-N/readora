package com.readora.sharedcore.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Validates JWTs issued by auth-service using the shared signing secret — every service except
 * auth-service itself (the real issuer, with its own JwtService that also knows about User
 * entities and role assignment — not a fit for this shared, domain-free module) and mcp-server
 * (which mints short-lived internal tokens via {@link #issueInternalToken}, not user-facing
 * ones) only ever validates.
 */
@Component
public class JwtService {

    private final SecretKey key;

    public JwtService(@Value("${app.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    /**
     * Verifies a token's signature and expiry, and extracts the caller's user id from its
     * subject claim.
     *
     * @param token the JWT string presented in the Authorization header
     * @return the user id if the token is valid, or empty if it's missing, expired, or the signature doesn't verify
     */
    public Optional<UUID> extractUserId(String token) {
        try {
            String subject = parseClaims(token).getSubject();
            return Optional.of(UUID.fromString(subject));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * Extracts the caller's role codes from a token's claims. Only call this after {@link
     * #extractUserId(String)} has confirmed the token verifies.
     *
     * @param token a signed JWT string
     * @return the roles claim, or an empty list if the token is invalid or carries no roles claim
     */
    public List<String> extractRoles(String token) {
        try {
            List<?> rawRoles = parseClaims(token).get("roles", List.class);
            return rawRoles == null ? List.of() : rawRoles.stream().map(String::valueOf).toList();
        } catch (JwtException | IllegalArgumentException e) {
            return List.of();
        }
    }

    /**
     * Extracts the caller's email from a token's claims. Only call this after {@link
     * #extractUserId(String)} has confirmed the token verifies.
     *
     * @param token a signed JWT string
     * @return the email claim, or empty if the token is invalid or carries no email claim
     */
    public Optional<String> extractEmail(String token) {
        try {
            Claims claims = parseClaims(token);
            return Optional.ofNullable(claims.get("email", String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * Verifies a token's signature and expiry.
     *
     * @param token a JWT string to validate
     * @return true if the token is signed with this service's key and not expired
     */
    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Signs a short-lived token whose subject is the given user id — used by mcp-server to carry
     * a userId that ai-service already authenticated through to a downstream service's own
     * JwtAuthenticationFilter, without that downstream service needing a separate internal-auth
     * mechanism. Expires in one minute; exists purely to satisfy the receiving filter.
     *
     * @param userId the user id to act on behalf of
     * @return a compact, signed JWT string valid for one minute
     */
    public String issueInternalToken(String userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(1, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
