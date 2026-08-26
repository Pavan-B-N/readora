package com.readora.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.payment.dto.OrderCancelledEvent;
import com.readora.payment.dto.OrderCreatedEvent;
import com.readora.payment.dto.OrderReturnedEvent;
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

import java.math.BigDecimal;
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

    /**
     * WALLET checkout already had its balance verified synchronously by commerce-service before
     * the order was created, so it's safe to capture immediately here. UPI has no real gateway —
     * it's simulated: authorize now, and {@link UpiSettlementJob} captures it a few seconds
     * later, entirely server-side over Kafka, so the frontend sees a genuine "pending, then
     * confirmed" flow rather than a fake client-side timer.
     */
    @Transactional
    public void handleOrderCreated(OrderCreatedEvent event) {
        String idempotencyKey = "order:" + event.orderId();

        if (paymentRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            return;
        }

        PaymentMethod method = PaymentMethod.valueOf(event.paymentMethod());
        Payment payment = new Payment(event.orderId(), event.userId(), method, event.grandTotal(), idempotencyKey);
        payment.authorize();

        if (method == PaymentMethod.WALLET) {
            payment.useWallet(event.walletAmountToUse());
            payment.capture();
            paymentRepository.save(payment);
            paymentAttemptRepository.save(new PaymentAttempt(payment, 1, payment.getStatus(), "dummy provider: auto-approved"));
            publishCaptured(payment);
        } else {
            paymentRepository.save(payment);
            paymentAttemptRepository.save(new PaymentAttempt(payment, 1, payment.getStatus(), "dummy UPI provider: awaiting settlement"));
        }
    }

    /** Called by {@link UpiSettlementJob} once a simulated UPI payment's settlement delay has elapsed. */
    @Transactional
    public void captureUpiPayment(Payment payment) {
        payment.capture();
        paymentRepository.save(payment);
        paymentAttemptRepository.save(new PaymentAttempt(payment, 2, payment.getStatus(), "dummy UPI provider: settled"));
        publishCaptured(payment);
    }

    private void publishCaptured(Payment payment) {
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
        refund(event.orderId(), event.userId(), event.reason(), event.refundAmount());
    }

    /** Same refund mechanics as cancellation — a return is just a later-stage cancellation, financially. */
    @Transactional
    public void handleOrderReturned(OrderReturnedEvent event) {
        refund(event.orderId(), event.userId(), event.reason(), event.refundAmount());
    }

    private void refund(UUID orderId, UUID userId, String reason, BigDecimal refundAmount) {
        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
        if (payment == null) {
            return;
        }

        Refund refund = new Refund(payment, refundAmount, reason);
        refund.complete();
        refundRepository.save(refund);

        payment.markRefunded();
        paymentRepository.save(payment);

        publish(
                "Refund",
                refund.getId(),
                KafkaTopics.REFUND_COMPLETED,
                new RefundCompletedEvent(orderId, refund.getId(), userId, refund.getAmount(), refund.getAmount())
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
