package com.readora.delivery.service;

import com.readora.delivery.entity.DeliveryAssignmentStatus;
import com.readora.delivery.entity.ReturnPickupStatus;
import com.readora.delivery.exception.AgentNotFoundException;
import com.readora.delivery.repository.DeliveryAgentRepository;
import com.readora.delivery.repository.DeliveryAssignmentRepository;
import com.readora.delivery.repository.ReturnPickupAssignmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentStatsServiceTest {

    @Mock private DeliveryAgentRepository agentRepository;
    @Mock private DeliveryAssignmentRepository assignmentRepository;
    @Mock private ReturnPickupAssignmentRepository pickupRepository;

    private AgentStatsService service;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new AgentStatsService(agentRepository, assignmentRepository, pickupRepository);
    }

    @Test
    void getStats_agentNotFound_throws() {
        when(agentRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getStats(userId)).isInstanceOf(AgentNotFoundException.class);
    }

    @Test
    void getStats_found_combinesDeliveryAndPickupEarnings() {
        when(agentRepository.findById(userId)).thenReturn(Optional.of(
                new com.readora.delivery.entity.DeliveryAgent(userId, "Agent Smith", "999", UUID.randomUUID())));
        when(assignmentRepository.countByAgentIdAndStatus(userId, DeliveryAssignmentStatus.DELIVERED)).thenReturn(5L);
        when(pickupRepository.countByAgentIdAndStatus(userId, ReturnPickupStatus.COLLECTED)).thenReturn(2L);
        when(assignmentRepository.sumPayoutByAgentIdAndStatus(userId, DeliveryAssignmentStatus.DELIVERED))
                .thenReturn(new BigDecimal("200.00"));
        when(pickupRepository.sumPayoutByAgentIdAndStatus(userId, ReturnPickupStatus.COLLECTED))
                .thenReturn(new BigDecimal("80.00"));

        var stats = service.getStats(userId);

        assertThat(stats.completedDeliveries()).isEqualTo(5);
        assertThat(stats.completedReturnPickups()).isEqualTo(2);
        assertThat(stats.totalEarnings()).isEqualByComparingTo("280.00");
    }
}
