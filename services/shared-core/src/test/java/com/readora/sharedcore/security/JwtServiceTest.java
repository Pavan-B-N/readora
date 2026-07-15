package com.readora.sharedcore.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = Base64.getEncoder().encodeToString("a-very-long-test-signing-secret-key-1234567890".getBytes());

    private JwtService jwtService;
    private SecretKey key;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET);
        key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
    }

    private String tokenFor(UUID userId, List<String> roles, String email, Instant expiry) {
        var builder = Jwts.builder()
                .subject(userId.toString())
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(expiry));
        if (roles != null) builder.claim("roles", roles);
        if (email != null) builder.claim("email", email);
        return builder.signWith(key).compact();
    }

    @Test
    void extractUserId_validToken_returnsTheSubject() {
        UUID userId = UUID.randomUUID();
        String token = tokenFor(userId, null, null, Instant.now().plusSeconds(60));

        assertThat(jwtService.extractUserId(token)).contains(userId);
    }

    @Test
    void extractUserId_expiredToken_returnsEmpty() {
        UUID userId = UUID.randomUUID();
        String token = tokenFor(userId, null, null, Instant.now().minusSeconds(60));

        assertThat(jwtService.extractUserId(token)).isEmpty();
    }

    @Test
    void extractUserId_garbageToken_returnsEmpty() {
        assertThat(jwtService.extractUserId("not-a-real-jwt")).isEmpty();
    }

    @Test
    void extractUserId_wrongSigningKey_returnsEmpty() {
        SecretKey otherKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(
                Base64.getEncoder().encodeToString("a-completely-different-signing-secret-key-000000".getBytes())));
        String token = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(otherKey)
                .compact();

        assertThat(jwtService.extractUserId(token)).isEmpty();
    }

    @Test
    void extractRoles_tokenWithRolesClaim_returnsThem() {
        UUID userId = UUID.randomUUID();
        String token = tokenFor(userId, List.of("ADMIN", "CUSTOMER"), null, Instant.now().plusSeconds(60));

        assertThat(jwtService.extractRoles(token)).containsExactly("ADMIN", "CUSTOMER");
    }

    @Test
    void extractRoles_tokenWithoutRolesClaim_returnsEmptyList() {
        UUID userId = UUID.randomUUID();
        String token = tokenFor(userId, null, null, Instant.now().plusSeconds(60));

        assertThat(jwtService.extractRoles(token)).isEmpty();
    }

    @Test
    void extractRoles_invalidToken_returnsEmptyList() {
        assertThat(jwtService.extractRoles("garbage")).isEmpty();
    }

    @Test
    void extractEmail_tokenWithEmailClaim_returnsIt() {
        UUID userId = UUID.randomUUID();
        String token = tokenFor(userId, null, "reader@example.com", Instant.now().plusSeconds(60));

        assertThat(jwtService.extractEmail(token)).contains("reader@example.com");
    }

    @Test
    void extractEmail_tokenWithoutEmailClaim_returnsEmpty() {
        UUID userId = UUID.randomUUID();
        String token = tokenFor(userId, null, null, Instant.now().plusSeconds(60));

        assertThat(jwtService.extractEmail(token)).isEmpty();
    }

    @Test
    void extractEmail_invalidToken_returnsEmpty() {
        assertThat(jwtService.extractEmail("garbage")).isEmpty();
    }

    @Test
    void isValid_validToken_true() {
        String token = tokenFor(UUID.randomUUID(), null, null, Instant.now().plusSeconds(60));
        assertThat(jwtService.isValid(token)).isTrue();
    }

    @Test
    void isValid_expiredToken_false() {
        String token = tokenFor(UUID.randomUUID(), null, null, Instant.now().minusSeconds(60));
        assertThat(jwtService.isValid(token)).isFalse();
    }

    @Test
    void isValid_garbageToken_false() {
        assertThat(jwtService.isValid("garbage")).isFalse();
    }

    @Test
    void issueInternalToken_mintsATokenValidForOneMinute() {
        String userId = UUID.randomUUID().toString();

        String token = jwtService.issueInternalToken(userId);

        assertThat(jwtService.extractUserId(token)).contains(UUID.fromString(userId));
        assertThat(jwtService.isValid(token)).isTrue();

        var claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        long secondsValid = ChronoUnit.SECONDS.between(claims.getIssuedAt().toInstant(), claims.getExpiration().toInstant());
        assertThat(secondsValid).isEqualTo(60);
    }
}
