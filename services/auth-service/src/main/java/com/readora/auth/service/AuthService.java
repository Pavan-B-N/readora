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
import com.readora.auth.repository.RoleRepository;
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

@Service
public class AuthService {

    // Repositories
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;


    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final int maxFailedLoginAttempts;
    private final long refreshTokenTtlDays;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            @Value("${app.security.max-failed-login-attempts}") int maxFailedLoginAttempts,
            @Value("${app.jwt.refresh-token-ttl-days}") long refreshTokenTtlDays
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
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
        roleRepository.findByCode(RoleCode.CUSTOMER).ifPresent(user::addRole);

        userRepository.save(user);

        return new RegisterResponse(user.getId(), user.getEmail(), user.getCreatedAt());
    }

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

    @Transactional
    public void logout(LogoutRequest request) {
        String presentedHash = hash(request.refreshToken());
        refreshTokenRepository.findByTokenHash(presentedHash).ifPresent(RefreshToken::revoke);
    }

    private String issueRefreshToken(User user, String userAgent, String ipAddress) {
        String rawToken = UUID.randomUUID().toString();
        String tokenHash = hash(rawToken);
        Instant expiresAt = Instant.now().plus(refreshTokenTtlDays, ChronoUnit.DAYS);

        RefreshToken refreshToken = new RefreshToken(user, tokenHash, expiresAt, userAgent, ipAddress);
        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

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
