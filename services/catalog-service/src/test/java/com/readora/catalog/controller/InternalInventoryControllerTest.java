package com.readora.catalog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.catalog.dto.ReserveStockRequest;
import com.readora.catalog.dto.ReserveStockResponse;
import com.readora.catalog.exception.GlobalExceptionHandler;
import com.readora.catalog.service.InventoryService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InternalInventoryControllerTest {

    @Mock private InventoryService inventoryService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new InternalInventoryController(inventoryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void reserve_valid_returns200() throws Exception {
        when(inventoryService.reserve(any())).thenReturn(new ReserveStockResponse(List.of()));

        mockMvc.perform(post("/internal/inventory/reserve")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new ReserveStockRequest(List.of(new ReserveStockRequest.Item(UUID.randomUUID(), 1))))))
                .andExpect(status().isOk());
    }

    @Test
    void reserve_emptyItems_returns400() throws Exception {
        mockMvc.perform(post("/internal/inventory/reserve")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new ReserveStockRequest(List.of()))))
                .andExpect(status().isBadRequest());
    }
}
