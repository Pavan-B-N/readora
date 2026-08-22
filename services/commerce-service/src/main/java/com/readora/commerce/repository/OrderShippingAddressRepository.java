package com.readora.commerce.repository;

import com.readora.commerce.entity.OrderShippingAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderShippingAddressRepository extends JpaRepository<OrderShippingAddress, UUID> {

    Optional<OrderShippingAddress> findByOrderId(UUID orderId);
}
