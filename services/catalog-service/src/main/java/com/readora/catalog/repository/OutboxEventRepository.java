package com.readora.catalog.repository;

import com.readora.catalog.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

// Spring Data JPA repository for OutboxEvent.
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    // Returns all events not yet published.
    List<OutboxEvent> findAllByPublishedAtIsNull();
}
