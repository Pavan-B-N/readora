package com.readora.auth.repository;

import com.readora.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Spring Data JPA repository for {@link User}. */
public interface UserRepository extends JpaRepository<User, UUID> {

    /** Used at login and for the pre-check during registration. */
    Optional<User> findByEmail(String email);

    /** Cheap existence check for an email address, without loading the full entity. */
    boolean existsByEmail(String email);
}
