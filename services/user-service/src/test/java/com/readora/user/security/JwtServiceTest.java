package com.readora.user.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = Base64.getEncoder().encodeToString(Keys.hmacShaKeyFor(
            "a-test-only-secret-that-is-long-enough-for-hs256".getBytes()).getEncoded());

    private JwtService jwtService;
    private SecretKey key;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET);
        key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET));
    }

    private String signToken(UUID subject, String email, Instant expiry) {
        var builder = Jwts.builder()
                .subject(subject == null ? null : subject.toString())
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(expiry));
        if (email != null) {
            builder.claim("email", email);
        }
        return builder.signWith(key).compact();
    }

    @Test
    void extractUserId_validToken_returnsTheSubjectAsUuid() {
        UUID userId = UUID.randomUUID();
        String token = signToken(userId, "reader@example.com", Instant.now().plusSeconds(3600));

        assertThat(jwtService.extractUserId(token)).contains(userId);
    }

    @Test
    void extractUserId_expiredToken_returnsEmpty() {
        UUID userId = UUID.randomUUID();
        String token = signToken(userId, "reader@example.com", Instant.now().minusSeconds(1));

        assertThat(jwtService.extractUserId(token)).isEmpty();
    }

    @Test
    void extractUserId_garbageString_returnsEmpty() {
        assertThat(jwtService.extractUserId("not-a-jwt")).isEmpty();
    }

    @Test
    void extractUserId_wrongSigningKey_returnsEmpty() {
        SecretKey otherKey = Keys.hmacShaKeyFor("a-completely-different-test-secret-of-sufficient-length".getBytes());
        String token = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(otherKey)
                .compact();

        assertThat(jwtService.extractUserId(token)).isEmpty();
    }

    @Test
    void extractEmail_validToken_returnsTheEmailClaim() {
        UUID userId = UUID.randomUUID();
        String token = signToken(userId, "reader@example.com", Instant.now().plusSeconds(3600));

        assertThat(jwtService.extractEmail(token)).contains("reader@example.com");
    }

    @Test
    void extractEmail_tokenWithoutEmailClaim_returnsEmpty() {
        UUID userId = UUID.randomUUID();
        String token = signToken(userId, null, Instant.now().plusSeconds(3600));

        assertThat(jwtService.extractEmail(token)).isEmpty();
    }

    @Test
    void extractEmail_invalidToken_returnsEmpty() {
        assertThat(jwtService.extractEmail("garbage")).isEmpty();
    }
}
