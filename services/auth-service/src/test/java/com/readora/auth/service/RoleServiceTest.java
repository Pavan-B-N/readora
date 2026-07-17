package com.readora.auth.service;

import com.readora.auth.entity.Role;
import com.readora.auth.entity.RoleCode;
import com.readora.auth.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    private RoleService roleService;

    @Test
    void getOrCreate_existingRole_returnsItWithoutSaving() {
        roleService = new RoleService(roleRepository);
        Role existing = new Role(RoleCode.CUSTOMER, "Customer");
        when(roleRepository.findByCode(RoleCode.CUSTOMER)).thenReturn(Optional.of(existing));

        Role result = roleService.getOrCreate(RoleCode.CUSTOMER);

        assertThat(result).isSameAs(existing);
        verify(roleRepository, never()).save(any());
    }

    @Test
    void getOrCreate_missingRole_createsItWithTheEnumDescription() {
        roleService = new RoleService(roleRepository);
        when(roleRepository.findByCode(RoleCode.CUSTOMER)).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Role result = roleService.getOrCreate(RoleCode.CUSTOMER);

        assertThat(result.getCode()).isEqualTo(RoleCode.CUSTOMER);
        assertThat(result.getDescription()).isEqualTo(RoleCode.CUSTOMER.getDescription());
    }
}
