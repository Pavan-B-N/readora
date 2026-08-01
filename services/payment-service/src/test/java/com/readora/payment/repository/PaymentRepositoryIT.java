package com.readora.payment.repository;

import com.readora.payment.entity.Payment;
import com.readora.payment.entity.PaymentMethod;
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

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PaymentRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine");

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void duplicateIdempotencyKey_violatesTheRealUniqueConstraint() {
        String idempotencyKey = "order:" + UUID.randomUUID();
        paymentRepository.saveAndFlush(new Payment(UUID.randomUUID(), UUID.randomUUID(), PaymentMethod.WALLET, new BigDecimal("500.00"), idempotencyKey));

        assertThatThrownBy(() -> paymentRepository.saveAndFlush(
                new Payment(UUID.randomUUID(), UUID.randomUUID(), PaymentMethod.WALLET, new BigDecimal("500.00"), idempotencyKey)
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void duplicateOrderId_violatesTheRealUniqueConstraint() {
        UUID orderId = UUID.randomUUID();
        paymentRepository.saveAndFlush(new Payment(orderId, UUID.randomUUID(), PaymentMethod.WALLET, new BigDecimal("500.00"), "order:" + UUID.randomUUID()));

        assertThatThrownBy(() -> paymentRepository.saveAndFlush(
                new Payment(orderId, UUID.randomUUID(), PaymentMethod.WALLET, new BigDecimal("500.00"), "order:" + UUID.randomUUID())
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findByOrderId_persistsAndReloadsThroughTheRealSchema() {
        UUID orderId = UUID.randomUUID();
        Payment saved = paymentRepository.saveAndFlush(new Payment(orderId, UUID.randomUUID(), PaymentMethod.UPI, new BigDecimal("250.00"), "order:" + UUID.randomUUID()));

        assertThat(paymentRepository.findByOrderId(orderId)).isPresent().get()
                .extracting(Payment::getId).isEqualTo(saved.getId());
    }
}
