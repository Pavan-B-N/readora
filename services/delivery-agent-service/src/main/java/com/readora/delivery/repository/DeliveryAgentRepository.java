package com.readora.delivery.repository;

import com.readora.delivery.entity.DeliveryAgent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DeliveryAgentRepository extends JpaRepository<DeliveryAgent, UUID> {

    List<DeliveryAgent> findAllByStoreId(UUID storeId);
}
