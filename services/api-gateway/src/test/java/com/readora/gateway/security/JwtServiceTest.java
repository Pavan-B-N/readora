package com.readora.gateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.time.Instant;
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

    private String signToken(UUID subject, Instant expiry) {
        return Jwts.builder()
                .subject(subject == null ? null : subject.toString())
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    @Test
    void isValid_validToken_isTrue() {
        assertThat(jwtService.isValid(signToken(UUID.randomUUID(), Instant.now().plusSeconds(3600)))).isTrue();
    }

    @Test
    void isValid_expiredToken_isFalse() {
        assertThat(jwtService.isValid(signToken(UUID.randomUUID(), Instant.now().minusSeconds(1)))).isFalse();
    }

    @Test
    void isValid_garbageString_isFalse() {
        assertThat(jwtService.isValid("not-a-jwt")).isFalse();
    }

    @Test
    void isValid_wrongSigningKey_isFalse() {
        SecretKey otherKey = Keys.hmacShaKeyFor("a-completely-different-test-secret-of-sufficient-length".getBytes());
        String token = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(otherKey)
                .compact();

        assertThat(jwtService.isValid(token)).isFalse();
    }

    @Test
    void extractUserId_validToken_returnsTheSubjectAsUuid() {
        UUID userId = UUID.randomUUID();
        String token = signToken(userId, Instant.now().plusSeconds(3600));

        assertThat(jwtService.extractUserId(token)).contains(userId);
    }

    @Test
    void extractUserId_invalidToken_returnsEmpty() {
        assertThat(jwtService.extractUserId("garbage")).isEmpty();
    }
}
