package com.readora.commerce.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.commerce.dto.AdminOrderSummaryResponse;
import com.readora.commerce.dto.ReviewOrderRequest;
import com.readora.commerce.exception.AdminOrderNotFoundException;
import com.readora.commerce.exception.GlobalExceptionHandler;
import com.readora.commerce.service.AdminOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminOrderControllerTest {

    @Mock private AdminOrderService adminOrderService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminOrderController(adminOrderService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static AdminOrderSummaryResponse summary() {
        return new AdminOrderSummaryResponse(UUID.randomUUID(), "RDA-2026-000001", "RETURNED",
                new BigDecimal("100.00"), "INR", Instant.now(), Instant.now(), "reason",
                "COMPLETED", new BigDecimal("100.00"), Instant.now(), Instant.now(), "note");
    }

    @Test
    void listReturns_delegatesToService() throws Exception {
        when(adminOrderService.listReturns(any())).thenReturn(new PageImpl<>(List.of(summary()), org.springframework.data.domain.PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/admin/orders")).andExpect(status().isOk());
    }

    @Test
    void listPendingReturns_delegatesToService() throws Exception {
        when(adminOrderService.listPendingReturns(any())).thenReturn(new PageImpl<>(List.of(), org.springframework.data.domain.PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/v1/admin/orders/pending")).andExpect(status().isOk());
    }

    @Test
    void listReviewedReturns_delegatesToService() throws Exception {
        when(adminOrderService.listReviewedReturns(any())).thenReturn(new PageImpl<>(List.of(), org.springframework.data.domain.PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/v1/admin/orders/reviewed")).andExpect(status().isOk());
    }

    @Test
    void getReturn_notFound_mapsTo404() throws Exception {
        when(adminOrderService.getReturn(any())).thenThrow(new AdminOrderNotFoundException());

        mockMvc.perform(get("/api/v1/admin/orders/" + UUID.randomUUID())).andExpect(status().isNotFound());
    }

    @Test
    void review_blankNote_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/admin/orders/" + UUID.randomUUID() + "/review")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new ReviewOrderRequest("", "APPROVE"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void review_valid_returns200() throws Exception {
        when(adminOrderService.reviewOrder(any(), any(), any())).thenReturn(summary());

        mockMvc.perform(post("/api/v1/admin/orders/" + UUID.randomUUID() + "/review")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new ReviewOrderRequest("looks good", "APPROVE"))))
                .andExpect(status().isOk());
    }

    @Test
    void returnMessages_delegatesToService() throws Exception {
        when(adminOrderService.listReturnMessages(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/admin/orders/" + UUID.randomUUID() + "/return/messages")).andExpect(status().isOk());
    }
}
