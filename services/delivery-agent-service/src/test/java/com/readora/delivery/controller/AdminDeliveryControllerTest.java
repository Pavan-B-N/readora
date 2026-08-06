package com.readora.delivery.controller;

import com.readora.delivery.exception.AdminStoreNotAssignedException;
import com.readora.sharedcore.exception.GlobalExceptionHandler;
import com.readora.delivery.service.AdminDeliveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminDeliveryControllerTest {

    @Mock private AdminDeliveryService adminDeliveryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminDeliveryController(adminDeliveryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listAgents_delegatesToService() throws Exception {
        when(adminDeliveryService.listAgents()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/admin/delivery/agents")).andExpect(status().isOk());
    }

    @Test
    void listAgents_noStoreAssigned_mapsTo403() throws Exception {
        when(adminDeliveryService.listAgents()).thenThrow(new AdminStoreNotAssignedException());

        mockMvc.perform(get("/api/v1/admin/delivery/agents")).andExpect(status().isForbidden());
    }
}
