package com.readora.auth.repository;

import com.readora.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Spring Data JPA repository for {@link User}. */
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Looks up a user by email — used at login and for the pre-check during registration.
     *
     * @param email the email address to search for
     * @return the matching user, or empty if no account has that email
     */
    Optional<User> findByEmail(String email);

    /**
     * Cheap existence check for an email address, without loading the full entity.
     *
     * @param email the email address to check
     * @return true if an account with that email already exists
     */
    boolean existsByEmail(String email);
}
