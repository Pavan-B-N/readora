package com.readora.payment.repository;

import com.readora.payment.entity.Payment;
import com.readora.payment.entity.PaymentMethod;
import com.readora.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByOrderId(UUID orderId);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    List<Payment> findAllByStatusAndMethodAndAuthorizedAtBefore(PaymentStatus status, PaymentMethod method, Instant cutoff);
}
