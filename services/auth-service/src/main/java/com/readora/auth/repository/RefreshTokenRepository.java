package com.readora.auth.repository;

import com.readora.auth.entity.RefreshToken;
import com.readora.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Spring Data JPA repository for {@link RefreshToken}. */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /** Used to validate a presented token during /auth/refresh and /auth/logout. */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Used to revoke a user's whole session family when refresh-token reuse is detected. */
    List<RefreshToken> findAllByUserAndRevokedAtIsNull(User user);
}
