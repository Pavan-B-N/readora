package com.readora.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.sharedcore.event.OrderCancelledEvent;
import com.readora.sharedcore.event.OrderCreatedEvent;
import com.readora.sharedcore.event.OrderReturnedEvent;
import com.readora.sharedcore.event.PaymentCapturedEvent;
import com.readora.payment.dto.PaymentResponse;
import com.readora.sharedcore.event.RefundCompletedEvent;
import com.readora.payment.dto.RefundStatusResponse;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Dummy payment provider: every authorization/capture succeeds immediately, no real gateway
 * call. This is intentional per project scope, not a placeholder left unfinished — replacing it
 * with a real provider integration later only touches this class.
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

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
            log.debug("Ignoring redelivered order.created for order {} — payment already exists", event.orderId());
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
            log.info("Captured WALLET payment {} for order {} (amount={})", payment.getId(), event.orderId(), event.grandTotal());
            publishCaptured(payment);
        } else {
            paymentRepository.save(payment);
            paymentAttemptRepository.save(new PaymentAttempt(payment, 1, payment.getStatus(), "dummy UPI provider: awaiting settlement"));
            log.info("Authorized UPI payment {} for order {}, awaiting settlement", payment.getId(), event.orderId());
        }
    }

    /** Called by {@link UpiSettlementJob} once a simulated UPI payment's settlement delay has elapsed. */
    @Transactional
    public void captureUpiPayment(Payment payment) {
        payment.capture();
        paymentRepository.save(payment);
        paymentAttemptRepository.save(new PaymentAttempt(payment, 2, payment.getStatus(), "dummy UPI provider: settled"));
        log.info("Captured UPI payment {} for order {} after settlement", payment.getId(), payment.getOrderId());
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
            log.warn("No payment found for order {} — cannot refund (reason: {})", orderId, reason);
            return;
        }

        // Kafka delivers order.cancelled/order.returned at-least-once — without this guard, a
        // redelivery would mint a second Refund row (fresh UUID) and double-credit the wallet,
        // since downstream idempotency in user-service dedupes by refundId, which would differ.
        if (refundRepository.findByPayment_Id(payment.getId()).isPresent()) {
            log.debug("Ignoring redelivered cancel/return for order {} — already refunded", orderId);
            return;
        }

        Refund refund = new Refund(payment, refundAmount, reason);
        refund.complete();
        refundRepository.save(refund);

        payment.markRefunded();
        paymentRepository.save(payment);

        log.info("Refunded {} for order {} (refund {}, reason: {})", refundAmount, orderId, refund.getId(), reason);

        publish(
                "Refund",
                refund.getId(),
                KafkaTopics.REFUND_COMPLETED,
                new RefundCompletedEvent(orderId, refund.getId(), userId, refund.getAmount(), refund.getAmount())
        );
    }

    /**
     * Internal-only lookup — no ownership check. Safe only because the sole caller,
     * InternalPaymentController, is reachable exclusively service-to-service (GatewaySecretFilter,
     * not per-caller auth) and the calling service (commerce-service) has already verified the
     * requesting user owns the order before asking for its payment details. The public endpoint
     * (PaymentController) must never call this overload directly — see getByOrderId(orderId, callerId).
     */
    @Transactional(readOnly = true)
    public PaymentResponse getByOrderId(UUID orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(PaymentNotFoundException::new);
        return toResponse(payment);
    }

    /**
     * Ownership-checked variant for the public /api/v1/payments/{orderId} endpoint. Without the
     * ownership filter here, any authenticated user could view another user's payment amount,
     * wallet-funded amount, method, and status just by passing an arbitrary order id. Failing with
     * the same PaymentNotFoundException (404) for both "no such payment" and "not yours" means a
     * caller can't distinguish the two by probing order ids.
     */
    @Transactional(readOnly = true)
    public PaymentResponse getByOrderId(UUID orderId, UUID callerId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .filter(p -> p.getUserId().equals(callerId))
                .orElseThrow(PaymentNotFoundException::new);
        return toResponse(payment);
    }

    private PaymentResponse toResponse(Payment payment) {
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

    /**
     * Batch lookup for commerce-service's admin returns view — one round trip for a whole page
     * of orders rather than N+1 internal calls. Orders with no refund row yet (event not
     * processed by this service, or none applicable) are simply absent from the result.
     */
    @Transactional(readOnly = true)
    public List<RefundStatusResponse> getRefundStatuses(List<UUID> orderIds) {
        if (orderIds.isEmpty()) {
            return List.of();
        }
        return refundRepository.findAllByPayment_OrderIdIn(orderIds).stream()
                .map(r -> new RefundStatusResponse(r.getPayment().getOrderId(), r.getStatus().name(), r.getAmount(), r.getCompletedAt()))
                .toList();
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
