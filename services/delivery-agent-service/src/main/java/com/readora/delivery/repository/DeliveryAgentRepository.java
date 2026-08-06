package com.readora.delivery.repository;

import com.readora.delivery.entity.DeliveryAgent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

// CRUD access to delivery agents.
public interface DeliveryAgentRepository extends JpaRepository<DeliveryAgent, UUID> {

    // Returns all agents belonging to the given store.
    List<DeliveryAgent> findAllByStoreId(UUID storeId);
}
