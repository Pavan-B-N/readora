package com.readora.commerce.repository;

import com.readora.commerce.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

// Data access for the transactional outbox.
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    // Every event still waiting to be published to Kafka.
    List<OutboxEvent> findAllByPublishedAtIsNull();
}
