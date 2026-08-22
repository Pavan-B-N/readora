package com.readora.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.payment.dto.OrderCancelledEvent;
import com.readora.payment.dto.OrderCreatedEvent;
import com.readora.payment.dto.PaymentCapturedEvent;
import com.readora.payment.dto.PaymentResponse;
import com.readora.payment.dto.RefundCompletedEvent;
import com.readora.payment.entity.OutboxEvent;
import com.readora.payment.entity.Payment;
import com.readora.payment.entity.PaymentAttempt;
import com.readora.payment.entity.PaymentMethod;
import com.readora.payment.entity.Refund;
import com.readora.payment.exception.PaymentNotFoundException;
import com.readora.payment.kafka.KafkaTopics;
import com.readora.payment.repository.OutboxEventRepository;
import com.readora.payment.repository.PaymentAttemptRepository;
import com.readora.payment.repository.PaymentRepository;
import com.readora.payment.repository.RefundRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Dummy payment provider: every authorization/capture succeeds immediately, no real gateway
 * call. This is intentional per project scope, not a placeholder left unfinished — replacing it
 * with a real provider integration later only touches this class.
 */
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final RefundRepository refundRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentAttemptRepository paymentAttemptRepository,
            RefundRepository refundRepository,
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.refundRepository = refundRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void handleOrderCreated(OrderCreatedEvent event) {
        String idempotencyKey = "order:" + event.orderId();

        if (paymentRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            return;
        }

        PaymentMethod method = PaymentMethod.valueOf(event.paymentMethod());
        Payment payment = new Payment(event.orderId(), event.userId(), method, event.grandTotal(), idempotencyKey);
        payment.authorize();
        payment.capture();
        paymentRepository.save(payment);

        paymentAttemptRepository.save(
                new PaymentAttempt(payment, 1, payment.getStatus(), "dummy provider: auto-approved")
        );

        publish(
                "Payment",
                payment.getId(),
                KafkaTopics.PAYMENT_CAPTURED,
                new PaymentCapturedEvent(
                        payment.getOrderId(), payment.getId(), payment.getUserId(),
                        payment.getAmount(), payment.getWalletAmountUsed()
                )
        );
    }

    @Transactional
    public void handleOrderCancelled(OrderCancelledEvent event) {
        Payment payment = paymentRepository.findByOrderId(event.orderId()).orElse(null);
        if (payment == null) {
            return;
        }

        Refund refund = new Refund(payment, event.refundAmount(), event.reason());
        refund.complete();
        refundRepository.save(refund);

        payment.markRefunded();
        paymentRepository.save(payment);

        publish(
                "Refund",
                refund.getId(),
                KafkaTopics.REFUND_COMPLETED,
                new RefundCompletedEvent(event.orderId(), refund.getId(), event.userId(), refund.getAmount(), refund.getAmount())
        );
    }

    @Transactional(readOnly = true)
    public PaymentResponse getByOrderId(UUID orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(PaymentNotFoundException::new);

        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getStatus(),
                payment.getMethod(),
                payment.getAmount(),
                payment.getWalletAmountUsed(),
                payment.getAuthorizedAt(),
                payment.getCapturedAt(),
                null
        );
    }

    private void publish(String aggregateType, UUID aggregateId, String topic, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            outboxEventRepository.save(new OutboxEvent(aggregateType, aggregateId, topic, json));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox event payload", e);
        }
    }
}
