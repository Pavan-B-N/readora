package com.readora.auth.service;

import com.readora.auth.dto.LoginRequest;
import com.readora.auth.dto.LoginResponse;
import com.readora.auth.dto.LogoutRequest;
import com.readora.auth.dto.RefreshRequest;
import com.readora.auth.dto.RefreshResponse;
import com.readora.auth.dto.RegisterRequest;
import com.readora.auth.entity.RefreshToken;
import com.readora.auth.entity.RoleCode;
import com.readora.auth.entity.User;
import com.readora.auth.entity.UserStatus;
import com.readora.auth.exception.AccountLockedException;
import com.readora.auth.exception.EmailAlreadyRegisteredException;
import com.readora.auth.exception.InvalidCredentialsException;
import com.readora.auth.exception.RefreshTokenInvalidException;
import com.readora.auth.exception.RefreshTokenReusedException;
import com.readora.auth.repository.RefreshTokenRepository;
import com.readora.auth.repository.UserRepository;
import com.readora.auth.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleService roleService;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository, roleService, refreshTokenRepository, passwordEncoder, jwtService,
                MAX_FAILED_LOGIN_ATTEMPTS, 30L
        );
    }

    @Test
    void register_throwsWhenEmailAlreadyRegistered() {
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(Boolean.valueOf(true));

        assertThatThrownBy(() -> authService.register(new RegisterRequest("taken@example.com", "password123", "A Name")))
                .isInstanceOf(EmailAlreadyRegisteredException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_wrongPassword_incrementsFailedAttemptsWithoutLocking() {
        User user = new User("reader@example.com", "hashed");
        when(userRepository.findByEmail("reader@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(Boolean.valueOf(false));

        assertThatThrownBy(() -> authService.login(new LoginRequest("reader@example.com", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);

        assertThat(user.getFailedLoginAttempts()).isEqualTo(1);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void login_wrongPassword_locksAccountOnceMaxAttemptsReached() {
        User user = new User("reader@example.com", "hashed");
        for (int i = 0; i < MAX_FAILED_LOGIN_ATTEMPTS - 1; i++) {
            user.incrementFailedLoginAttempts();
        }
        when(userRepository.findByEmail("reader@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(Boolean.valueOf(false));

        assertThatThrownBy(() -> authService.login(new LoginRequest("reader@example.com", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);

        assertThat(user.getFailedLoginAttempts()).isEqualTo(MAX_FAILED_LOGIN_ATTEMPTS);
        assertThat(user.getStatus()).isEqualTo(UserStatus.LOCKED);
    }

    @Test
    void login_alreadyLockedAccount_rejectsBeforeCheckingPassword() {
        User user = new User("reader@example.com", "hashed");
        user.setStatus(UserStatus.LOCKED);
        when(userRepository.findByEmail("reader@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("reader@example.com", "whatever")))
                .isInstanceOf(AccountLockedException.class);

        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void login_correctPassword_resetsFailedAttemptsAndIssuesTokens() {
        User user = new User("reader@example.com", "hashed");
        user.incrementFailedLoginAttempts();
        user.incrementFailedLoginAttempts();
        when(userRepository.findByEmail("reader@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct", "hashed")).thenReturn(Boolean.valueOf(true));
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(jwtService.getAccessTokenTtlSeconds()).thenReturn(Long.valueOf(900L));

        LoginResponse response = authService.login(new LoginRequest("reader@example.com", "correct"));

        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.expiresIn()).isEqualTo(900L);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void refresh_reusedRevokedToken_revokesEveryOtherActiveTokenAndRejects() {
        User user = new User("reader@example.com", "hashed");
        RefreshToken revoked = new RefreshToken(user, "hash-of-presented-token", Instant.now().plusSeconds(3600), null, null);
        revoked.revoke();
        RefreshToken stillActive = new RefreshToken(user, "hash-of-other-token", Instant.now().plusSeconds(3600), null, null);

        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(revoked));
        when(refreshTokenRepository.findAllByUserAndRevokedAtIsNull(user)).thenReturn(List.of(stillActive));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("presented-raw-token")))
                .isInstanceOf(RefreshTokenReusedException.class);

        assertThat(stillActive.isRevoked()).isTrue();
        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    void register_newEmail_hashesPasswordAndAssignsCustomerRole() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(Boolean.valueOf(false));
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(roleService.getOrCreate(RoleCode.CUSTOMER)).thenReturn(new com.readora.auth.entity.Role(RoleCode.CUSTOMER, "Customer"));

        authService.register(new RegisterRequest("new@example.com", "password123", "A Name"));

        verify(userRepository, times(1)).save(any(User.class));
        verify(roleService).getOrCreate(RoleCode.CUSTOMER);
    }

    @Test
    void refresh_unknownToken_throwsInvalidBeforeIssuingAnything() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("unknown-token")))
                .isInstanceOf(RefreshTokenInvalidException.class);

        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    void refresh_expiredButNotRevokedToken_throwsInvalidWithoutRevokingOthers() {
        User user = new User("reader@example.com", "hashed");
        RefreshToken expired = new RefreshToken(user, "hash", Instant.now().minusSeconds(1), null, null);

        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("expired-token")))
                .isInstanceOf(RefreshTokenInvalidException.class);

        verify(refreshTokenRepository, never()).findAllByUserAndRevokedAtIsNull(any());
        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    void refresh_validToken_revokesItAndIssuesANewPairCarryingForwardDeviceInfo() {
        User user = new User("reader@example.com", "hashed");
        RefreshToken existing = new RefreshToken(user, "hash", Instant.now().plusSeconds(3600), "some-agent", "10.0.0.1");

        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(existing));
        when(jwtService.generateAccessToken(user)).thenReturn("new-access-token");
        when(jwtService.getAccessTokenTtlSeconds()).thenReturn(900L);

        RefreshResponse response = authService.refresh(new RefreshRequest("presented-token"));

        assertThat(existing.isRevoked()).isTrue();
        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.expiresIn()).isEqualTo(900L);
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    void logout_knownToken_revokesIt() {
        User user = new User("reader@example.com", "hashed");
        RefreshToken existing = new RefreshToken(user, "hash", Instant.now().plusSeconds(3600), null, null);
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(existing));

        authService.logout(new LogoutRequest("presented-token"));

        assertThat(existing.isRevoked()).isTrue();
    }

    @Test
    void logout_unknownToken_silentlyNoOps() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        authService.logout(new LogoutRequest("unknown-token"));

        verify(refreshTokenRepository, never()).save(any());
    }
}
