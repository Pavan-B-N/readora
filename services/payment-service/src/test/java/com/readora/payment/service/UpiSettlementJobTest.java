package com.readora.payment.service;

import com.readora.payment.entity.Payment;
import com.readora.payment.entity.PaymentMethod;
import com.readora.payment.entity.PaymentStatus;
import com.readora.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpiSettlementJobTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentService paymentService;

    private UpiSettlementJob job;

    @Test
    void settle_noDuePayments_isANoOp() {
        job = new UpiSettlementJob(paymentRepository, paymentService);
        when(paymentRepository.findAllByStatusAndMethodAndAuthorizedAtBefore(any(), any(), any())).thenReturn(List.of());

        job.settle();

        verify(paymentService, never()).captureUpiPayment(any());
    }

    @Test
    void settle_duePayments_capturesEach() {
        job = new UpiSettlementJob(paymentRepository, paymentService);
        Payment payment = new Payment(UUID.randomUUID(), UUID.randomUUID(), PaymentMethod.UPI, new BigDecimal("500.00"), "order:x");
        when(paymentRepository.findAllByStatusAndMethodAndAuthorizedAtBefore(
                eq(PaymentStatus.AUTHORIZED), eq(PaymentMethod.UPI), any())).thenReturn(List.of(payment));

        job.settle();

        verify(paymentService, times(1)).captureUpiPayment(payment);
    }

    private static <T> T eq(T value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }
}
