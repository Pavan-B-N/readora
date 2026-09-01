package com.readora.payment.controller;

import com.readora.payment.dto.PaymentResponse;
import com.readora.payment.entity.PaymentMethod;
import com.readora.payment.entity.PaymentStatus;
import com.readora.payment.exception.GlobalExceptionHandler;
import com.readora.payment.exception.PaymentNotFoundException;
import com.readora.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InternalPaymentControllerTest {

    @Mock private PaymentService paymentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new InternalPaymentController(paymentService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getByOrder_found_returns200() throws Exception {
        when(paymentService.getByOrderId(any())).thenReturn(new PaymentResponse(
                UUID.randomUUID(), UUID.randomUUID(), PaymentStatus.CAPTURED, PaymentMethod.WALLET,
                new BigDecimal("500.00"), new BigDecimal("500.00"), null, null, null));

        mockMvc.perform(get("/internal/payments/" + UUID.randomUUID())).andExpect(status().isOk());
    }

    @Test
    void getByOrder_notFound_mapsTo404() throws Exception {
        when(paymentService.getByOrderId(any())).thenThrow(new PaymentNotFoundException());

        mockMvc.perform(get("/internal/payments/" + UUID.randomUUID())).andExpect(status().isNotFound());
    }
}
