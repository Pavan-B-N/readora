package com.readora.delivery.controller;

import com.readora.sharedcore.exception.GlobalExceptionHandler;
import com.readora.delivery.exception.ReturnPickupNotFoundException;
import com.readora.sharedcore.security.CurrentUserContext;
import com.readora.delivery.service.ReturnPickupService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReturnPickupControllerTest {

    @Mock private ReturnPickupService returnPickupService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ReturnPickupController(returnPickupService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        CurrentUserContext.set(UUID.randomUUID(), List.of("DELIVERY_AGENT"));
    }

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test
    void queue_delegatesToService() throws Exception {
        when(returnPickupService.getQueue(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/returns/queue")).andExpect(status().isOk());
    }

    @Test
    void mine_delegatesToService() throws Exception {
        when(returnPickupService.getMine(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/returns/mine")).andExpect(status().isOk());
    }

    @Test
    void detail_found_returns200() throws Exception {
        when(returnPickupService.getDetail(any(), any())).thenReturn(
                new com.readora.delivery.dto.ReturnPickupDetailResponse(null, null)
        );

        mockMvc.perform(get("/api/v1/returns/" + UUID.randomUUID())).andExpect(status().isOk());
    }

    @Test
    void detail_notFound_mapsTo404() throws Exception {
        when(returnPickupService.getDetail(any(), any())).thenThrow(new ReturnPickupNotFoundException());

        mockMvc.perform(get("/api/v1/returns/" + UUID.randomUUID())).andExpect(status().isNotFound());
    }

    @Test
    void claim_delegatesToService() throws Exception {
        mockMvc.perform(post("/api/v1/returns/" + UUID.randomUUID() + "/claim")).andExpect(status().isOk());
    }

    @Test
    void enRoute_delegatesToService() throws Exception {
        mockMvc.perform(post("/api/v1/returns/" + UUID.randomUUID() + "/en-route")).andExpect(status().isOk());
    }

    @Test
    void collected_delegatesToService() throws Exception {
        mockMvc.perform(post("/api/v1/returns/" + UUID.randomUUID() + "/collected")).andExpect(status().isOk());
    }
}
