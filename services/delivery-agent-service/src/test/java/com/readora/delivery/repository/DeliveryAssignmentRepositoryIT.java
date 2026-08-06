package com.readora.delivery.repository;

import com.readora.delivery.entity.DeliveryAssignment;
import com.readora.delivery.entity.DeliveryAssignmentStatus;
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

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DeliveryAssignmentRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine");

    @Autowired
    private DeliveryAssignmentRepository assignmentRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void duplicateOrderId_violatesTheRealUniqueConstraint() {
        UUID orderId = UUID.randomUUID();
        assignmentRepository.saveAndFlush(new DeliveryAssignment(
                orderId, "ORD-1", UUID.randomUUID(), "Bengaluru", "Ravi Kumar", "9999999999", "[{\"title\":\"Clean Code\",\"qty\":1}]", new BigDecimal("40.00")
        ));

        assertThatThrownBy(() -> assignmentRepository.saveAndFlush(new DeliveryAssignment(
                orderId, "ORD-2", UUID.randomUUID(), "Mumbai", "Asha Rao", "8888888888", "[{\"title\":\"The Pragmatic Programmer\",\"qty\":1}]", new BigDecimal("40.00")
        ))).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void claim_persistsTheAgentAndAssignedStatusThroughARealRoundTrip() {
        UUID agentId = UUID.randomUUID();
        DeliveryAssignment assignment = assignmentRepository.saveAndFlush(
                new DeliveryAssignment(UUID.randomUUID(), "ORD-3", UUID.randomUUID(), "Delhi", "Priya Singh", "7777777777", "[{\"title\":\"Clean Architecture\",\"qty\":2}]", new BigDecimal("40.00"))
        );

        assignment.claim(agentId);
        assignmentRepository.saveAndFlush(assignment);
        entityManager.clear();

        DeliveryAssignment reloaded = assignmentRepository.findById(assignment.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(DeliveryAssignmentStatus.ASSIGNED);
        assertThat(reloaded.getAgentId()).isEqualTo(agentId);
    }
}
