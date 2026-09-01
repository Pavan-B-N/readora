package com.readora.ai.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
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

    private String signToken(UUID subject, List<String> roles, Instant expiry) {
        var builder = Jwts.builder()
                .subject(subject == null ? null : subject.toString())
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(expiry));
        if (roles != null) {
            builder.claim("roles", roles);
        }
        return builder.signWith(key).compact();
    }

    @Test
    void extractUserId_validToken_returnsTheSubjectAsUuid() {
        UUID userId = UUID.randomUUID();
        String token = signToken(userId, List.of("CUSTOMER"), Instant.now().plusSeconds(3600));

        assertThat(jwtService.extractUserId(token)).contains(userId);
    }

    @Test
    void extractUserId_expiredToken_returnsEmpty() {
        String token = signToken(UUID.randomUUID(), List.of("CUSTOMER"), Instant.now().minusSeconds(1));

        assertThat(jwtService.extractUserId(token)).isEmpty();
    }

    @Test
    void extractUserId_garbageString_returnsEmpty() {
        assertThat(jwtService.extractUserId("not-a-jwt")).isEmpty();
    }

    @Test
    void extractRoles_validToken_returnsTheRolesClaim() {
        String token = signToken(UUID.randomUUID(), List.of("CUSTOMER", "ADMIN"), Instant.now().plusSeconds(3600));

        assertThat(jwtService.extractRoles(token)).containsExactlyInAnyOrder("CUSTOMER", "ADMIN");
    }

    @Test
    void extractRoles_tokenWithoutRolesClaim_returnsEmptyList() {
        String token = signToken(UUID.randomUUID(), null, Instant.now().plusSeconds(3600));

        assertThat(jwtService.extractRoles(token)).isEmpty();
    }

    @Test
    void extractRoles_invalidToken_returnsEmptyList() {
        assertThat(jwtService.extractRoles("garbage")).isEmpty();
    }
}
