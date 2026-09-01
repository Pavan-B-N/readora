package com.readora.payment.controller;

import com.readora.payment.dto.RefundStatusResponse;
import com.readora.payment.service.PaymentService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InternalRefundControllerTest {

    @Mock private PaymentService paymentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new InternalRefundController(paymentService)).build();
    }

    @Test
    void byOrderIds_delegatesToService() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(paymentService.getRefundStatuses(any())).thenReturn(
                List.of(new RefundStatusResponse(orderId, "COMPLETED", new BigDecimal("500.00"), null)));

        mockMvc.perform(get("/internal/refunds/by-order-ids").param("orderIds", orderId.toString()))
                .andExpect(status().isOk());
    }
}
