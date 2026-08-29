package com.readora.auth.repository;

import com.readora.auth.entity.User;
import com.readora.auth.entity.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs against a real, throwaway Postgres container (schema created by the actual V1 Flyway
 * migration, not Hibernate) — verifies constraints Mockito-backed unit tests structurally can't:
 * the real unique-email constraint, and that Flyway + Hibernate's mapping actually agree.
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void savingTwoUsersWithTheSameEmail_violatesTheRealUniqueConstraint() {
        userRepository.saveAndFlush(new User("duplicate@example.com", "hash-one"));

        assertThatThrownBy(() -> userRepository.saveAndFlush(new User("duplicate@example.com", "hash-two")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findByEmail_persistsAndReloadsThroughTheRealSchema() {
        User saved = userRepository.saveAndFlush(new User("reader@example.com", "hashed"));
        entityManager.clear();

        assertThat(userRepository.findByEmail("reader@example.com"))
                .isPresent()
                .get()
                .satisfies(found -> {
                    assertThat(found.getId()).isEqualTo(saved.getId());
                    assertThat(found.getStatus()).isEqualTo(UserStatus.ACTIVE);
                    assertThat(found.getFailedLoginAttempts()).isZero();
                });
    }
}
