package com.readora.auth.security;

import com.readora.auth.entity.Role;
import com.readora.auth.entity.RoleCode;
import com.readora.auth.entity.User;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = Base64.getEncoder().encodeToString(Keys.hmacShaKeyFor(
            "a-test-only-secret-that-is-long-enough-for-hs256".getBytes()).getEncoded());

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 15L);
    }

    @Test
    void generateAccessToken_roundTripsTheSameUserId() throws Exception {
        User user = new User("reader@example.com", "hashed");
        setId(user, UUID.randomUUID());
        user.addRole(new Role(RoleCode.CUSTOMER, "Customer"));

        String token = jwtService.generateAccessToken(user);

        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.getUserIdFromToken(token)).isEqualTo(user.getId());
    }

    @Test
    void getAccessTokenTtlSeconds_convertsMinutesToSeconds() {
        assertThat(jwtService.getAccessTokenTtlSeconds()).isEqualTo(15L * 60);
    }

    @Test
    void isTokenValid_garbageString_isFalse() {
        assertThat(jwtService.isTokenValid("not-a-real-jwt")).isFalse();
    }

    @Test
    void isTokenValid_tokenSignedWithADifferentKey_isFalse() throws Exception {
        JwtService otherService = new JwtService(
                Base64.getEncoder().encodeToString(Keys.hmacShaKeyFor(
                        "a-completely-different-test-secret-of-sufficient-length".getBytes()).getEncoded()),
                15L
        );
        User user = new User("reader@example.com", "hashed");
        setId(user, UUID.randomUUID());
        String tokenFromOtherKey = otherService.generateAccessToken(user);

        assertThat(jwtService.isTokenValid(tokenFromOtherKey)).isFalse();
    }

    @Test
    void isTokenValid_expiredToken_isFalse() throws Exception {
        JwtService alreadyExpired = new JwtService(SECRET, -1L);
        User user = new User("reader@example.com", "hashed");
        setId(user, UUID.randomUUID());

        String token = alreadyExpired.generateAccessToken(user);

        assertThat(jwtService.isTokenValid(token)).isFalse();
    }

    private static void setId(User user, UUID id) throws Exception {
        Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(user, id);
    }
}
