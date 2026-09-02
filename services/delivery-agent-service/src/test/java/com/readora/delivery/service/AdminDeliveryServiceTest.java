package com.readora.delivery.service;

import com.readora.delivery.client.UserServiceClient;
import com.readora.delivery.entity.DeliveryAgent;
import com.readora.delivery.entity.DeliveryAssignment;
import com.readora.delivery.entity.DeliveryAssignmentStatus;
import com.readora.delivery.entity.ReturnPickupAssignment;
import com.readora.delivery.exception.AdminStoreNotAssignedException;
import com.readora.delivery.repository.DeliveryAgentRepository;
import com.readora.delivery.repository.DeliveryAssignmentRepository;
import com.readora.delivery.repository.ReturnPickupAssignmentRepository;
import com.readora.sharedcore.security.CurrentUserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDeliveryServiceTest {

    @Mock private DeliveryAgentRepository agentRepository;
    @Mock private DeliveryAssignmentRepository assignmentRepository;
    @Mock private ReturnPickupAssignmentRepository pickupRepository;
    @Mock private UserServiceClient userServiceClient;

    private AdminDeliveryService service;
    private final UUID adminId = UUID.randomUUID();
    private final UUID storeId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new AdminDeliveryService(agentRepository, assignmentRepository, pickupRepository, userServiceClient);
        CurrentUserContext.set(adminId, List.of("ADMIN"));
    }

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test
    void listAgents_noStoreAssigned_throws() {
        when(userServiceClient.getAdminStoreId(adminId)).thenReturn(null);

        assertThatThrownBy(() -> service.listAgents()).isInstanceOf(AdminStoreNotAssignedException.class);
    }

    @Test
    void listAgents_agentWithNoActiveWork_returnsNullActiveWork() {
        when(userServiceClient.getAdminStoreId(adminId)).thenReturn(storeId);
        DeliveryAgent agent = new DeliveryAgent(UUID.randomUUID(), "Agent Smith", "999", storeId);
        when(agentRepository.findAllByStoreId(storeId)).thenReturn(List.of(agent));
        when(assignmentRepository.findAllByAgentIdOrderByCreatedAtDesc(agent.getUserId())).thenReturn(List.of());
        when(pickupRepository.findAllByAgentIdOrderByCreatedAtDesc(agent.getUserId())).thenReturn(List.of());

        var agents = service.listAgents();

        assertThat(agents.get(0).activeWork()).isNull();
    }

    @Test
    void listAgents_agentWithActiveDelivery_reportsDeliveryActiveWork() {
        when(userServiceClient.getAdminStoreId(adminId)).thenReturn(storeId);
        DeliveryAgent agent = new DeliveryAgent(UUID.randomUUID(), "Agent Smith", "999", storeId);
        DeliveryAssignment assignment = new DeliveryAssignment(UUID.randomUUID(), "ORD-1", storeId,
                "Bengaluru", "Ravi Kumar", "999", "[]", new BigDecimal("40.00"));
        assignment.claim(agent.getUserId());
        when(agentRepository.findAllByStoreId(storeId)).thenReturn(List.of(agent));
        when(assignmentRepository.findAllByAgentIdOrderByCreatedAtDesc(agent.getUserId())).thenReturn(List.of(assignment));

        var agents = service.listAgents();

        assertThat(agents.get(0).activeWork().type()).isEqualTo("DELIVERY");
    }

    @Test
    void listAgents_agentWithActiveReturnPickupOnly_reportsPickupActiveWork() {
        when(userServiceClient.getAdminStoreId(adminId)).thenReturn(storeId);
        DeliveryAgent agent = new DeliveryAgent(UUID.randomUUID(), "Agent Smith", "999", storeId);
        ReturnPickupAssignment pickup = new ReturnPickupAssignment(UUID.randomUUID(), "ORD-2", storeId,
                "Chennai", "Asha Rao", "888", "[]", new BigDecimal("40.00"));
        pickup.claim(agent.getUserId(), "Agent Smith");
        when(agentRepository.findAllByStoreId(storeId)).thenReturn(List.of(agent));
        when(assignmentRepository.findAllByAgentIdOrderByCreatedAtDesc(agent.getUserId())).thenReturn(List.of());
        when(pickupRepository.findAllByAgentIdOrderByCreatedAtDesc(agent.getUserId())).thenReturn(List.of(pickup));

        var agents = service.listAgents();

        assertThat(agents.get(0).activeWork().type()).isEqualTo("RETURN_PICKUP");
    }

    @Test
    void listAgents_deliveredAssignmentIsNotActiveWork() {
        when(userServiceClient.getAdminStoreId(adminId)).thenReturn(storeId);
        DeliveryAgent agent = new DeliveryAgent(UUID.randomUUID(), "Agent Smith", "999", storeId);
        DeliveryAssignment delivered = new DeliveryAssignment(UUID.randomUUID(), "ORD-3", storeId,
                "Bengaluru", "Ravi Kumar", "999", "[]", new BigDecimal("40.00"));
        delivered.claim(agent.getUserId());
        delivered.markOutForDelivery();
        delivered.markDelivered();
        when(agentRepository.findAllByStoreId(storeId)).thenReturn(List.of(agent));
        when(assignmentRepository.findAllByAgentIdOrderByCreatedAtDesc(agent.getUserId())).thenReturn(List.of(delivered));
        when(pickupRepository.findAllByAgentIdOrderByCreatedAtDesc(agent.getUserId())).thenReturn(List.of());

        var agents = service.listAgents();

        assertThat(agents.get(0).activeWork()).isNull();
    }
}
