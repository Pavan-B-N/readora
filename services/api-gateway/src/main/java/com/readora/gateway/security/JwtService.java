package com.readora.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Validates JWTs issued by auth-service using the shared signing secret — the gateway never issues its own tokens.
 * Extracts the caller's user id and email from a valid token's claims.
 */
@Component
public class JwtService {

    private final SecretKey key;

    public JwtService(@Value("${app.jwt.secret}") String secret) {
        // Creates an HMAC verification key
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    /**
     * Verifies a token's signature and expiry, and extracts the caller's identity from its claims.
     *
     * @param token the JWT string presented in the Authorization header
     * @return the authenticated principal (user id and email) if the token is valid, or empty if it's missing, expired, or the signature doesn't verify
     */
    public Optional<AuthenticatedPrincipal> validate(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            UUID userId = UUID.fromString(claims.getSubject());
            String email = claims.get("email", String.class);

            List<?> rawRoles = claims.get("roles", List.class);
            List<String> roles = rawRoles == null
                    ? List.of()
                    : rawRoles.stream().map(String::valueOf).toList();

            return Optional.of(new AuthenticatedPrincipal(userId, email, roles));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
