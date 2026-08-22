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

/** Registration, login, refresh-token rotation and logout — the business logic behind {@link com.readora.auth.controller.AuthController}. */
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

    /**
     * @param userRepository            persists and queries user accounts
     * @param roleRepository            looks up roles to assign at registration
     * @param refreshTokenRepository    persists and queries refresh tokens
     * @param passwordEncoder           hashes and verifies passwords
     * @param jwtService                signs access tokens
     * @param maxFailedLoginAttempts    number of consecutive failed logins before an account is locked
     * @param refreshTokenTtlDays       how many days an issued refresh token remains valid
     */
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

    /**
     * Creates a new account with the CUSTOMER role, hashing the submitted password before it's
     * ever persisted.
     *
     * @param request the registration request (email, password, full name)
     * @return the newly created account's id, email, and creation timestamp
     * @throws EmailAlreadyRegisteredException if an account already exists for the email
     */
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

    /**
     * Verifies credentials and, on success, issues a new access/refresh token pair. Unknown
     * email and wrong password both fail with the same {@link InvalidCredentialsException} so
     * the endpoint can't be used to enumerate registered accounts.
     *
     * @param request the login request (email, password)
     * @return a new access token, refresh token, and the access token's lifetime
     * @throws InvalidCredentialsException if the email is unknown or the password is wrong
     * @throws AccountLockedException      if the account is currently locked
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
     * Rotates the refresh token: the submitted token is revoked before the replacement pair is
     * issued, so a stolen token can only ever be used once. If a revoked token is presented again
     * (reuse), every other active token for that user is revoked too, killing the whole session.
     *
     * @param request the refresh request, carrying the raw refresh token
     * @return a new access token, a new refresh token, and the access token's lifetime
     * @throws RefreshTokenInvalidException if the token is unknown or expired
     * @throws RefreshTokenReusedException  if a previously-revoked token is presented again
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

    /**
     * Revokes the supplied refresh token, if it exists. Silently no-ops for an unknown token —
     * logout is not a place to leak whether a token was ever valid.
     *
     * @param request the logout request, carrying the raw refresh token to revoke
     */
    @Transactional
    public void logout(LogoutRequest request) {
        String presentedHash = hash(request.refreshToken());
        refreshTokenRepository.findByTokenHash(presentedHash).ifPresent(RefreshToken::revoke);
    }

    /**
     * Generates a new raw refresh token, stores only its hash, and returns the raw value —
     * the only time the raw value ever exists outside the client.
     *
     * @param user      the user to issue the token for
     * @param userAgent the requesting client's User-Agent header, or null
     * @param ipAddress the requesting client's IP address, or null
     * @return the raw (unhashed) refresh token to return to the caller
     */
    private String issueRefreshToken(User user, String userAgent, String ipAddress) {
        String rawToken = UUID.randomUUID().toString();
        String tokenHash = hash(rawToken);
        Instant expiresAt = Instant.now().plus(refreshTokenTtlDays, ChronoUnit.DAYS);

        RefreshToken refreshToken = new RefreshToken(user, tokenHash, expiresAt, userAgent, ipAddress);
        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    /**
     * Hashes a raw refresh token value with SHA-256 for storage/lookup. Unlike password hashing,
     * this doesn't need to be slow or salted — the raw value is already 128 bits of random
     * entropy (a UUID), so a fast deterministic hash is both sufficient and required for
     * indexed exact-match lookup by hash.
     *
     * @param value the raw value to hash
     * @return the Base64-encoded SHA-256 digest of the value
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
