package com.readora.mcp.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Base64;

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

    @Test
    void issueInternalToken_signsATokenWithTheGivenUserIdAsSubject() {
        String token = jwtService.issueInternalToken("some-user-id");

        String subject = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
        assertThat(subject).isEqualTo("some-user-id");
    }

    @Test
    void issueInternalToken_expiresOneMinuteAfterIssuance() {
        String token = jwtService.issueInternalToken("some-user-id");

        var claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        long ttlMillis = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        assertThat(ttlMillis).isEqualTo(60_000L);
    }

    @Test
    void issueInternalToken_isVerifiableOnlyWithTheSameKey() {
        String token = jwtService.issueInternalToken("some-user-id");
        SecretKey otherKey = Keys.hmacShaKeyFor("a-completely-different-test-secret-of-sufficient-length".getBytes());

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                Jwts.parser().verifyWith(otherKey).build().parseSignedClaims(token)
        ).isInstanceOf(io.jsonwebtoken.security.SignatureException.class);
    }
}
