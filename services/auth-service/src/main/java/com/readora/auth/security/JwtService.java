package com.readora.auth.security;

import com.readora.auth.entity.User;
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
import java.util.UUID;

/** Signs and validates the HS256 JWTs this service issues. */
@Component
public class JwtService {

    private final SecretKey key;
    private final long accessTokenTtlMinutes;

    /**
     * Builds the HMAC signing key from the configured Base64 secret.
     *
     * @param secret                the Base64-encoded shared signing secret (also known to api-gateway)
     * @param accessTokenTtlMinutes how many minutes an issued access token remains valid
     */
    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-ttl-minutes}") long accessTokenTtlMinutes
    ) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessTokenTtlMinutes = accessTokenTtlMinutes;
    }

    /**
     * Signs a new access token for a user, with the user's id as the subject claim and their
     * email as a custom claim.
     *
     * @param user the user to issue a token for
     * @return a compact, signed JWT string
     */
    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(accessTokenTtlMinutes, ChronoUnit.MINUTES);

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    /** @return the configured access token lifetime, in seconds */
    public long getAccessTokenTtlSeconds() {
        return accessTokenTtlMinutes * 60;
    }

    /**
     * Extracts the user id from a token's subject claim. Only call this after {@link
     * #isTokenValid(String)} has confirmed the token verifies.
     *
     * @param token a signed JWT string
     * @return the user id encoded in the token's subject claim
     */
    public UUID getUserIdFromToken(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    /**
     * Checks whether a token's signature verifies and it has not expired.
     *
     * @param token a JWT string to validate
     * @return true if the token is signed with this service's key and not expired
     */
    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Parses and verifies a token's signature, throwing if it's invalid or expired.
     *
     * @param token a JWT string to parse
     * @return the token's claims payload
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
