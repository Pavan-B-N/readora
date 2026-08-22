package com.readora.notification.security;

import io.jsonwebtoken.Claims;
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
 * Validates JWTs issued by auth-service using the shared signing secret, so STOMP CONNECT
 * frames can be authenticated directly — this WebSocket endpoint isn't routed through
 * api-gateway, so there's no gateway-forwarded identity header to trust instead.
 */
@Component
public class JwtService {

    private final SecretKey key;

    public JwtService(@Value("${app.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    /**
     * Verifies a token's signature and expiry and extracts the caller's user id.
     *
     * @param token the raw JWT string, without the "Bearer " prefix
     * @return the caller's user id if the token is valid, or empty if it's missing, expired, or the signature doesn't verify
     */
    public Optional<UUID> validate(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return Optional.of(UUID.fromString(claims.getSubject()));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
