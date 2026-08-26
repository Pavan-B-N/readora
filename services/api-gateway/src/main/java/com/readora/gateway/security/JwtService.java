package com.readora.gateway.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Optional;
import java.util.UUID;

/**
 * Validates JWTs issued by auth-service using the shared signing secret — the gateway never
 * issues its own tokens. The gateway only needs to accept or reject a request, so it does not
 * parse or forward any claims; each downstream service extracts what it needs from the token
 * itself.
 */
@Component
public class JwtService {

    private final SecretKey key;

    public JwtService(@Value("${app.jwt.secret}") String secret) {
        // Creates an HMAC verification key
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    /**
     * Verifies a token's signature and expiry.
     *
     * @param token the JWT string presented in the Authorization header
     * @return true if the token is signed with this service's key and not expired
     */
    public boolean isValid(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Verifies a token's signature and expiry, and extracts the caller's user id from its
     * subject claim. Used by the rate limiter to key authenticated callers by user id rather
     * than IP.
     *
     * @param token the JWT string presented in the Authorization header
     * @return the user id if the token is valid, or empty if it's missing, expired, or the signature doesn't verify
     */
    public Optional<UUID> extractUserId(String token) {
        try {
            String subject = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
            return Optional.of(UUID.fromString(subject));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
