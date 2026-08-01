package com.readora.payment.repository;

import com.readora.payment.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

// Spring Data JPA repository for {@link OutboxEvent}.
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    // Used by the outbox relay to find events still waiting to be published.
    List<OutboxEvent> findAllByPublishedAtIsNull();
}
