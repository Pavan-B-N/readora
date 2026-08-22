package com.readora.auth.repository;

import com.readora.auth.entity.RefreshToken;
import com.readora.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Spring Data JPA repository for {@link RefreshToken}. */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Looks up a refresh token by the hash of its raw value — used to validate a presented
     * token during /auth/refresh and /auth/logout.
     *
     * @param tokenHash the SHA-256 hash of the raw token value
     * @return the matching token, or empty if no token has that hash
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Finds every currently-active (unrevoked) token for a user — used to revoke a user's whole
     * session family when refresh-token reuse is detected.
     *
     * @param user the user whose active tokens to find
     * @return every token belonging to the user that has not been revoked
     */
    List<RefreshToken> findAllByUserAndRevokedAtIsNull(User user);
}
