package com.readora.commerce.repository;

import com.readora.commerce.entity.OrderShippingAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

// Data access for order shipping-address snapshots.
public interface OrderShippingAddressRepository extends JpaRepository<OrderShippingAddress, UUID> {

    // The address snapshot for one order.
    Optional<OrderShippingAddress> findByOrderId(UUID orderId);
}
