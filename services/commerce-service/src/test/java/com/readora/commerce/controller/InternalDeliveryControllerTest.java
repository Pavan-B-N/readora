package com.readora.commerce.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.commerce.dto.OrderDeliveryDetailResponse;
import com.readora.commerce.dto.UpdateDeliveryStatusRequest;
import com.readora.commerce.dto.UpdateReturnStatusRequest;
import com.readora.commerce.entity.OrderStatus;
import com.readora.sharedcore.exception.GlobalExceptionHandler;
import com.readora.commerce.exception.InvalidDeliveryTransitionException;
import com.readora.commerce.exception.OrderNotFoundException;
import com.readora.commerce.service.OrderFulfillmentService;
import com.readora.commerce.service.OrderQueryService;
import com.readora.commerce.service.ReturnService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InternalDeliveryControllerTest {

    @Mock private OrderQueryService orderQueryService;
    @Mock private OrderFulfillmentService orderFulfillmentService;
    @Mock private ReturnService returnService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new InternalDeliveryController(orderQueryService, orderFulfillmentService, returnService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getDeliveryDetail_notFound_mapsTo404() throws Exception {
        when(orderQueryService.getDeliveryDetail(any())).thenThrow(new OrderNotFoundException());

        mockMvc.perform(get("/internal/orders/" + UUID.randomUUID() + "/delivery-detail"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getDeliveryDetail_found_returns200() throws Exception {
        when(orderQueryService.getDeliveryDetail(any())).thenReturn(
                new OrderDeliveryDetailResponse(UUID.randomUUID(), "RDA-2026-000001", "CONFIRMED", UUID.randomUUID(),
                        null, List.of(), java.math.BigDecimal.TEN, java.time.Instant.now()));

        mockMvc.perform(get("/internal/orders/" + UUID.randomUUID() + "/delivery-detail"))
                .andExpect(status().isOk());
    }

    @Test
    void updateDeliveryStatus_illegalTransition_mapsTo409() throws Exception {
        org.mockito.Mockito.doThrow(new InvalidDeliveryTransitionException())
                .when(orderFulfillmentService).updateDeliveryStatus(any(), any(), any(), any());

        mockMvc.perform(put("/internal/orders/" + UUID.randomUUID() + "/delivery-status")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new UpdateDeliveryStatusRequest(OrderStatus.ASSIGNED, UUID.randomUUID(), "Agent"))))
                .andExpect(status().isConflict());
    }

    @Test
    void updateReturnStatus_valid_returns204() throws Exception {
        mockMvc.perform(put("/internal/orders/" + UUID.randomUUID() + "/return-status")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new UpdateReturnStatusRequest(OrderStatus.RETURN_ASSIGNED, UUID.randomUUID(), "Agent"))))
                .andExpect(status().isNoContent());
    }
}
