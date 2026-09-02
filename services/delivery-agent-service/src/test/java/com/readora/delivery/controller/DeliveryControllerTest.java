package com.readora.delivery.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.delivery.dto.AgentMeResponse;
import com.readora.delivery.dto.AgentStatsResponse;
import com.readora.delivery.dto.SetDutyRequest;
import com.readora.delivery.exception.AssignmentAlreadyClaimedException;
import com.readora.sharedcore.exception.GlobalExceptionHandler;
import com.readora.sharedcore.security.CurrentUserContext;
import com.readora.delivery.service.AgentStatsService;
import com.readora.delivery.service.DeliveryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DeliveryControllerTest {

    @Mock private DeliveryService deliveryService;
    @Mock private AgentStatsService agentStatsService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new DeliveryController(deliveryService, agentStatsService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        CurrentUserContext.set(UUID.randomUUID(), List.of("DELIVERY_AGENT"));
    }

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test
    void me_delegatesToService() throws Exception {
        when(deliveryService.getMe(any())).thenReturn(new AgentMeResponse(UUID.randomUUID(), "Agent Smith", "999", UUID.randomUUID(), true));

        mockMvc.perform(get("/api/v1/delivery/me")).andExpect(status().isOk());
    }

    @Test
    void stats_delegatesToService() throws Exception {
        when(agentStatsService.getStats(any())).thenReturn(new AgentStatsResponse(1, 2, new BigDecimal("100.00"), "INR"));

        mockMvc.perform(get("/api/v1/delivery/me/stats")).andExpect(status().isOk());
    }

    @Test
    void setDuty_delegatesToService() throws Exception {
        when(deliveryService.setOnDuty(any(), org.mockito.ArgumentMatchers.anyBoolean()))
                .thenReturn(new AgentMeResponse(UUID.randomUUID(), "Agent Smith", "999", UUID.randomUUID(), true));

        mockMvc.perform(put("/api/v1/delivery/me/duty")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new SetDutyRequest(true))))
                .andExpect(status().isOk());
    }

    @Test
    void queue_delegatesToService() throws Exception {
        when(deliveryService.getQueue(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/delivery/queue")).andExpect(status().isOk());
    }

    @Test
    void mine_delegatesToService() throws Exception {
        when(deliveryService.getMine(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/delivery/mine")).andExpect(status().isOk());
    }

    @Test
    void claim_alreadyClaimed_mapsTo409() throws Exception {
        when(deliveryService.claim(any(), any())).thenThrow(new AssignmentAlreadyClaimedException());

        mockMvc.perform(post("/api/v1/delivery/" + UUID.randomUUID() + "/claim")).andExpect(status().isConflict());
    }

    @Test
    void outForDelivery_delegatesToService() throws Exception {
        mockMvc.perform(post("/api/v1/delivery/" + UUID.randomUUID() + "/out-for-delivery")).andExpect(status().isOk());
    }

    @Test
    void delivered_delegatesToService() throws Exception {
        mockMvc.perform(post("/api/v1/delivery/" + UUID.randomUUID() + "/delivered")).andExpect(status().isOk());
    }
}
