package com.readora.auth.repository;

import com.readora.auth.entity.Role;
import com.readora.auth.entity.RoleCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Spring Data JPA repository for {@link Role}. */
public interface RoleRepository extends JpaRepository<Role, UUID> {

    /** Used to assign the default CUSTOMER role at registration. */
    Optional<Role> findByCode(RoleCode code);
}
