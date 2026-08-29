package com.readora.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.payment.dto.OrderCreatedEvent;
import com.readora.payment.dto.OrderReturnedEvent;
import com.readora.payment.dto.PaymentResponse;
import com.readora.payment.entity.Payment;
import com.readora.payment.entity.PaymentMethod;
import com.readora.payment.entity.PaymentStatus;
import com.readora.payment.entity.Refund;
import com.readora.payment.exception.PaymentNotFoundException;
import com.readora.payment.repository.OutboxEventRepository;
import com.readora.payment.repository.PaymentAttemptRepository;
import com.readora.payment.repository.PaymentRepository;
import com.readora.payment.repository.RefundRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentAttemptRepository paymentAttemptRepository;
    @Mock
    private RefundRepository refundRepository;
    @Mock
    private OutboxEventRepository outboxEventRepository;

    private PaymentService paymentService;

    private final UUID orderId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository, paymentAttemptRepository, refundRepository, outboxEventRepository, new ObjectMapper());
    }

    @Test
    void handleOrderCreated_walletMethod_authorizesAndCapturesImmediately() {
        when(paymentRepository.findByIdempotencyKey("order:" + orderId)).thenReturn(Optional.empty());

        paymentService.handleOrderCreated(new OrderCreatedEvent(orderId, userId, List.of(), new BigDecimal("500.00"), new BigDecimal("500.00"), "WALLET"));

        var captor = org.mockito.ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.CAPTURED);
        verify(outboxEventRepository).save(any());
    }

    @Test
    void handleOrderCreated_upiMethod_staysAuthorizedAwaitingSettlement() {
        when(paymentRepository.findByIdempotencyKey("order:" + orderId)).thenReturn(Optional.empty());

        paymentService.handleOrderCreated(new OrderCreatedEvent(orderId, userId, List.of(), new BigDecimal("500.00"), BigDecimal.ZERO, "UPI"));

        var captor = org.mockito.ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        // No capture yet — UpiSettlementJob does that later — so no PAYMENT_CAPTURED event either.
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void handleOrderCreated_duplicateEvent_isIgnored() {
        Payment existing = new Payment(orderId, userId, PaymentMethod.WALLET, new BigDecimal("500.00"), "order:" + orderId);
        when(paymentRepository.findByIdempotencyKey("order:" + orderId)).thenReturn(Optional.of(existing));

        paymentService.handleOrderCreated(new OrderCreatedEvent(orderId, userId, List.of(), new BigDecimal("500.00"), new BigDecimal("500.00"), "WALLET"));

        verify(paymentRepository, never()).save(any());
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void handleOrderReturned_noPaymentOnRecord_isANoOp() {
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        paymentService.handleOrderReturned(new OrderReturnedEvent(orderId, userId, "damaged", new BigDecimal("500.00")));

        verify(refundRepository, never()).save(any());
    }

    @Test
    void handleOrderReturned_redeliveredEvent_doesNotDoubleRefund() {
        Payment payment = new Payment(orderId, userId, PaymentMethod.WALLET, new BigDecimal("500.00"), "order:" + orderId);
        payment.authorize();
        payment.capture();
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));
        when(refundRepository.findByPayment_Id(any())).thenReturn(Optional.of(new Refund(payment, new BigDecimal("500.00"), "damaged")));

        paymentService.handleOrderReturned(new OrderReturnedEvent(orderId, userId, "damaged", new BigDecimal("500.00")));

        verify(refundRepository, never()).save(any());
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void handleOrderReturned_firstTime_completesRefundAndMarksPaymentRefunded() {
        Payment payment = new Payment(orderId, userId, PaymentMethod.WALLET, new BigDecimal("500.00"), "order:" + orderId);
        payment.authorize();
        payment.capture();
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));
        when(refundRepository.findByPayment_Id(any())).thenReturn(Optional.empty());

        paymentService.handleOrderReturned(new OrderReturnedEvent(orderId, userId, "damaged", new BigDecimal("500.00")));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(refundRepository, times(1)).save(any(Refund.class));
        verify(outboxEventRepository).save(any());
    }

    @Test
    void getByOrderId_withCallerId_rejectsWhenCallerDoesNotOwnThePayment() {
        Payment payment = new Payment(orderId, userId, PaymentMethod.WALLET, new BigDecimal("500.00"), "order:" + orderId);
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

        UUID someoneElse = UUID.randomUUID();
        assertThatThrownBy(() -> paymentService.getByOrderId(orderId, someoneElse))
                .isInstanceOf(PaymentNotFoundException.class);
    }

    @Test
    void getByOrderId_withCallerId_returnsPaymentWhenOwnedByCaller() {
        Payment payment = new Payment(orderId, userId, PaymentMethod.WALLET, new BigDecimal("500.00"), "order:" + orderId);
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.getByOrderId(orderId, userId);

        assertThat(response.orderId()).isEqualTo(orderId);
    }
}
