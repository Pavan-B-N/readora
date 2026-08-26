package com.readora.mcp.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Mints short-lived internal JWTs using the same shared signing secret as auth-service, so
 * commerce-service and user-service can validate them the same way they validate a real user
 * token. Used only to carry the userId that ai-service already authenticated through to
 * mcp-server's downstream calls — the token is minted fresh per call and expires in a minute,
 * since it exists purely to satisfy the receiving service's JwtAuthenticationFilter.
 */
@Component
public class JwtService {

    private final SecretKey key;

    public JwtService(@Value("${app.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    /**
     * Signs a short-lived token whose subject is the given user id.
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
}
