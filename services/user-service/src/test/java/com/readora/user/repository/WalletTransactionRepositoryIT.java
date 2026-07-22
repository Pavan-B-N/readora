package com.readora.user.repository;

import com.readora.user.entity.WalletTransaction;
import com.readora.user.entity.WalletTransactionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Real Postgres via Flyway's V1-V3 migrations — in particular, this is the concrete regression
 * test for V3 (the CHECK constraint widened to allow CASHBACK): if that migration were ever
 * wrong or missing, the CASHBACK save below would fail with a real constraint violation.
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class WalletTransactionRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine");

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    @Test
    void cashbackTransactionType_isAcceptedByTheRealCheckConstraint() {
        WalletTransaction saved = walletTransactionRepository.saveAndFlush(new WalletTransaction(
                UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("10.00"), WalletTransactionType.CASHBACK,
                new BigDecimal("510.00"), "cashback:" + UUID.randomUUID()
        ));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getType()).isEqualTo(WalletTransactionType.CASHBACK);
    }

    @Test
    void duplicateIdempotencyKey_violatesTheRealUniqueConstraint() {
        String idempotencyKey = "payment:" + UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        walletTransactionRepository.saveAndFlush(new WalletTransaction(
                userId, UUID.randomUUID(), new BigDecimal("-50.00"), WalletTransactionType.REDEEMED, new BigDecimal("450.00"), idempotencyKey
        ));

        assertThatThrownBy(() -> walletTransactionRepository.saveAndFlush(new WalletTransaction(
                userId, UUID.randomUUID(), new BigDecimal("-50.00"), WalletTransactionType.REDEEMED, new BigDecimal("400.00"), idempotencyKey
        ))).isInstanceOf(DataIntegrityViolationException.class);
    }
}
