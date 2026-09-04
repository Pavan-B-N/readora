package com.readora.auth.service;

import com.readora.auth.dto.*;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

/** Registration, login, refresh-token rotation and logout — the business logic behind {@link com.readora.auth.controller.AuthController}. */
@Service
public class AuthService {

    // Repositories
    private final UserRepository userRepository;
    private final RoleService roleService;
    private final RefreshTokenRepository refreshTokenRepository;


    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final int maxFailedLoginAttempts;
    private final long refreshTokenTtlDays;

    public AuthService(
            UserRepository userRepository,
            RoleService roleService,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            @Value("${app.security.max-failed-login-attempts}") int maxFailedLoginAttempts,
            @Value("${app.jwt.refresh-token-ttl-days}") long refreshTokenTtlDays
    ) {
        this.userRepository = userRepository;
        this.roleService = roleService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.maxFailedLoginAttempts = maxFailedLoginAttempts;
        this.refreshTokenTtlDays = refreshTokenTtlDays;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyRegisteredException(request.email());
        }

        User user = new User(request.email(), passwordEncoder.encode(request.password()));
        user.addRole(roleService.getOrCreate(RoleCode.CUSTOMER));

        userRepository.save(user);

        return new RegisterResponse(user.getId(), user.getEmail(), user.getCreatedAt());
    }

    /**
     * Unknown email and wrong password both fail with the same {@link InvalidCredentialsException}
     * so the endpoint can't be used to enumerate registered accounts.
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (user.getStatus() == UserStatus.LOCKED) {
            throw new AccountLockedException();
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            user.incrementFailedLoginAttempts();
            if (user.getFailedLoginAttempts() >= maxFailedLoginAttempts) {
                user.setStatus(UserStatus.LOCKED);
            }
            userRepository.save(user);
            throw new InvalidCredentialsException();
        }

        user.resetFailedLoginAttempts();
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = issueRefreshToken(user, null, null);

        return new LoginResponse(accessToken, refreshToken, "Bearer", jwtService.getAccessTokenTtlSeconds());
    }

    /**
     * The submitted token is revoked before the replacement pair is issued, so a stolen token
     * can only ever be used once. If a revoked token is presented again (reuse), every other
     * active token for that user is revoked too, killing the whole session.
     */
    @Transactional
    public RefreshResponse refresh(RefreshRequest request) {
        String presentedHash = hash(request.refreshToken());

        RefreshToken existing = refreshTokenRepository.findByTokenHash(presentedHash)
                .orElseThrow(RefreshTokenInvalidException::new);

        if (existing.isRevoked()) {
            refreshTokenRepository.findAllByUserAndRevokedAtIsNull(existing.getUser())
                    .forEach(RefreshToken::revoke);
            throw new RefreshTokenReusedException();
        }

        if (existing.isExpired()) {
            throw new RefreshTokenInvalidException();
        }

        existing.revoke();
        refreshTokenRepository.save(existing);

        User user = existing.getUser();
        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = issueRefreshToken(user, existing.getUserAgent(), existing.getIpAddress());

        return new RefreshResponse(newAccessToken, newRefreshToken, jwtService.getAccessTokenTtlSeconds());
    }

    /** Silently no-ops for an unknown token — logout is not a place to leak whether one was ever valid. */
    @Transactional
    public void logout(LogoutRequest request) {
        String presentedHash = hash(request.refreshToken());
        refreshTokenRepository.findByTokenHash(presentedHash).ifPresent(RefreshToken::revoke);
    }

    /** Stores only the token's hash; the raw value returned here is the only time it exists outside the client. */
    private String issueRefreshToken(User user, String userAgent, String ipAddress) {
        String rawToken = UUID.randomUUID().toString();
        String tokenHash = hash(rawToken);
        Instant expiresAt = Instant.now().plus(refreshTokenTtlDays, ChronoUnit.DAYS);

        RefreshToken refreshToken = new RefreshToken(user, tokenHash, expiresAt, userAgent, ipAddress);
        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    /**
     * Unlike password hashing, this doesn't need to be slow or salted — the raw value is already
     * 128 bits of random entropy (a UUID), so a fast deterministic hash is both sufficient and
     * required for indexed exact-match lookup by hash.
     */
    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes());
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
