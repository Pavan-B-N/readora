package com.readora.payment.repository;

import com.readora.payment.entity.Payment;
import com.readora.payment.entity.PaymentMethod;
import com.readora.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Spring Data JPA repository for {@link Payment}.
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    // Used to look up the payment for a given order.
    Optional<Payment> findByOrderId(UUID orderId);

    // Used to dedupe repeated payment attempts by their client-supplied idempotency key.
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    // Used by the UPI settlement job to find authorized payments stuck past the cutoff.
    List<Payment> findAllByStatusAndMethodAndAuthorizedAtBefore(PaymentStatus status, PaymentMethod method, Instant cutoff);
}
