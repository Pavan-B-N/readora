package com.readora.auth.repository;

import com.readora.auth.entity.Role;
import com.readora.auth.entity.RoleCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Spring Data JPA repository for {@link Role}. */
public interface RoleRepository extends JpaRepository<Role, UUID> {

    /**
     * Looks up a role by its code — used to assign the default CUSTOMER role at registration.
     *
     * @param code the role code to search for
     * @return the matching role, or empty if it doesn't exist (e.g. not yet seeded)
     */
    Optional<Role> findByCode(RoleCode code);
}
