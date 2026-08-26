package com.readora.payment.service;

import com.readora.payment.entity.Payment;
import com.readora.payment.entity.PaymentMethod;
import com.readora.payment.entity.PaymentStatus;
import com.readora.payment.repository.PaymentRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Simulates a real UPI provider's settlement delay entirely server-side: a UPI payment is
 * authorized immediately, then this poll captures it once it's been AUTHORIZED for at least
 * SETTLE_DELAY — a few seconds later, same as a real customer approving a UPI collect request
 * on their phone. Poll cadence matches {@link OutboxRelay}, so actual settlement lands roughly
 * SETTLE_DELAY to SETTLE_DELAY+POLL_INTERVAL after authorization (comfortably inside the
 * intended 5-10s window). Deliberately a scheduled poll over Kafka-published events, not a
 * frontend timer — the frontend only ever learns the outcome from payment.captured.
 */
@Component
public class UpiSettlementJob {

    private static final Duration SETTLE_DELAY = Duration.ofSeconds(6);

    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    public UpiSettlementJob(PaymentRepository paymentRepository, PaymentService paymentService) {
        this.paymentRepository = paymentRepository;
        this.paymentService = paymentService;
    }

    @Scheduled(fixedDelay = 2000)
    public void settle() {
        Instant cutoff = Instant.now().minus(SETTLE_DELAY);
        List<Payment> due = paymentRepository.findAllByStatusAndMethodAndAuthorizedAtBefore(
                PaymentStatus.AUTHORIZED, PaymentMethod.UPI, cutoff
        );

        for (Payment payment : due) {
            paymentService.captureUpiPayment(payment);
        }
    }
}
