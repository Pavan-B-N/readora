package com.readora.payment.repository;

import com.readora.payment.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefundRepository extends JpaRepository<Refund, UUID> {

    /** Guards against double-refunding the same payment on a redelivered order.cancelled/order.returned event. */
    Optional<Refund> findByPayment_Id(UUID paymentId);

    List<Refund> findAllByPayment_OrderIdIn(List<UUID> orderIds);
}
