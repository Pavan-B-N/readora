package com.readora.auth.service;

import com.readora.auth.entity.Role;
import com.readora.auth.entity.RoleCode;
import com.readora.auth.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The single place role lookup/creation logic lives — roles are created lazily on first use, rather than seeded at startup. */
@Service
public class RoleService {

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    /**
     * Returns the role for the given code, creating it with the code's enum-defined description
     * if it doesn't exist yet.
     *
     * @param code the role code to look up
     * @return the existing or newly created role
     */
    @Transactional
    public Role getOrCreate(RoleCode code) {
        return roleRepository.findByCode(code)
                .orElseGet(() -> roleRepository.save(new Role(code, code.getDescription())));
    }
}
