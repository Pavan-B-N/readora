package com.readora.commerce.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.readora.commerce.dto.CancelOrderRequest;
import com.readora.commerce.dto.CancelOrderResponse;
import com.readora.commerce.dto.CheckoutRequest;
import com.readora.commerce.dto.CheckoutResponse;
import com.readora.commerce.dto.PostReturnMessageRequest;
import com.readora.commerce.dto.ReturnOrderRequest;
import com.readora.commerce.entity.DeliveryType;
import com.readora.sharedcore.exception.GlobalExceptionHandler;
import com.readora.commerce.exception.OrderNotFoundException;
import com.readora.sharedcore.security.CurrentUserContext;
import com.readora.commerce.service.CheckoutService;
import com.readora.commerce.service.OrderQueryService;
import com.readora.commerce.service.ReturnService;
import org.junit.jupiter.api.AfterEach;
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
class OrderControllerTest {

    @Mock private CheckoutService checkoutService;
    @Mock private OrderQueryService orderQueryService;
    @Mock private ReturnService returnService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new OrderController(checkoutService, orderQueryService, returnService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        CurrentUserContext.set(UUID.randomUUID(), List.of("CUSTOMER"));
    }

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test
    void checkout_valid_returns201() throws Exception {
        when(checkoutService.checkout(any(), any(), any())).thenReturn(new CheckoutResponse(
                UUID.randomUUID(), "RDA-2026-000001", "PENDING_PAYMENT", "VIRTUAL", BigDecimal.TEN, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ONE, new BigDecimal("11.00"), BigDecimal.ZERO, "WALLET", "INR", Instant.now()));

        CheckoutRequest request = new CheckoutRequest(null, "WALLET", null,
                List.of(new CheckoutRequest.Item(UUID.randomUUID(), 1, DeliveryType.VIRTUAL)));

        mockMvc.perform(post("/api/v1/orders/checkout")
                        .header("Idempotency-Key", "idem-1")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void checkout_emptyItems_returns400() throws Exception {
        CheckoutRequest request = new CheckoutRequest(null, "WALLET", null, List.of());

        mockMvc.perform(post("/api/v1/orders/checkout")
                        .header("Idempotency-Key", "idem-1")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void list_delegatesToService() throws Exception {
        when(orderQueryService.listOrders(any(), any())).thenReturn(new PageImpl<>(List.of(), org.springframework.data.domain.PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/v1/orders")).andExpect(status().isOk());
    }

    @Test
    void getDetail_notFound_mapsTo404() throws Exception {
        when(orderQueryService.getDetail(any(), any())).thenThrow(new OrderNotFoundException());

        mockMvc.perform(get("/api/v1/orders/" + UUID.randomUUID())).andExpect(status().isNotFound());
    }

    @Test
    void cancel_delegatesToService() throws Exception {
        when(returnService.cancel(any(), any(), any())).thenReturn(
                new CancelOrderResponse(UUID.randomUUID(), "CANCELLED", Instant.now()));

        mockMvc.perform(post("/api/v1/orders/" + UUID.randomUUID() + "/cancel")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CancelOrderRequest("changed mind"))))
                .andExpect(status().isOk());
    }

    @Test
    void returnOrder_blankReason_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/orders/" + UUID.randomUUID() + "/return")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new ReturnOrderRequest(""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnMessages_delegatesToService() throws Exception {
        when(returnService.listReturnMessages(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/orders/" + UUID.randomUUID() + "/return/messages")).andExpect(status().isOk());
    }

    @Test
    void postReturnMessage_blankContent_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/orders/" + UUID.randomUUID() + "/return/messages")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new PostReturnMessageRequest(""))))
                .andExpect(status().isBadRequest());
    }
}
