package com.readora.auth.config;

import com.readora.auth.entity.Role;
import com.readora.auth.entity.RoleCode;
import com.readora.auth.repository.RoleRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class RoleSeeder implements ApplicationRunner {

    private final RoleRepository roleRepository;

    public RoleSeeder(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedIfMissing(RoleCode.CUSTOMER, "Default role for registered customers");
        seedIfMissing(RoleCode.ADMIN, "Administrative role with elevated privileges");
    }

    private void seedIfMissing(RoleCode code, String description) {
        if (roleRepository.findByCode(code).isEmpty()) {
            roleRepository.save(new Role(code, description));
        }
    }
}
