package com.readora.sharedcore.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrentUserContextTest {

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void get_beforeSet_isEmpty() {
        assertThat(CurrentUserContext.get()).isEmpty();
    }

    @Test
    void require_beforeSet_throws() {
        assertThatThrownBy(CurrentUserContext::require).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void set_userIdOnly_defaultsRolesAndEmailToEmpty() {
        UUID userId = UUID.randomUUID();
        CurrentUserContext.set(userId);

        assertThat(CurrentUserContext.require()).isEqualTo(userId);
        assertThat(CurrentUserContext.hasRole("ADMIN")).isFalse();
        assertThat(CurrentUserContext.getEmail()).isEmpty();
    }

    @Test
    void set_withRoles_hasRoleReflectsThem() {
        UUID userId = UUID.randomUUID();
        CurrentUserContext.set(userId, List.of("ADMIN", "CUSTOMER"));

        assertThat(CurrentUserContext.hasRole("ADMIN")).isTrue();
        assertThat(CurrentUserContext.hasRole("DELIVERY_AGENT")).isFalse();
        assertThat(CurrentUserContext.getEmail()).isEmpty();
    }

    @Test
    void set_withRolesAndEmail_bothAreReadable() {
        UUID userId = UUID.randomUUID();
        CurrentUserContext.set(userId, List.of("CUSTOMER"), "reader@example.com");

        assertThat(CurrentUserContext.require()).isEqualTo(userId);
        assertThat(CurrentUserContext.hasRole("CUSTOMER")).isTrue();
        assertThat(CurrentUserContext.getEmail()).contains("reader@example.com");
    }

    @Test
    void clear_removesEverything() {
        CurrentUserContext.set(UUID.randomUUID(), List.of("ADMIN"), "reader@example.com");

        CurrentUserContext.clear();

        assertThat(CurrentUserContext.get()).isEmpty();
        assertThat(CurrentUserContext.hasRole("ADMIN")).isFalse();
        assertThat(CurrentUserContext.getEmail()).isEmpty();
    }
}
