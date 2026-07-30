package com.readora.commerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** One pending or published domain event, written in the same transaction as its business row. */
@Entity
@Table(name = "outbox_events", schema = "commerce")
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    // JPA no-arg constructor.
    protected OutboxEvent() {
    }

    // Creates a pending outbox row for a domain event.
    public OutboxEvent(String aggregateType, UUID aggregateId, String eventType, String payload) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
    }

    // Stamps the creation time before insert.
    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    // Marks this event as published now.
    public void markPublished() {
        this.publishedAt = Instant.now();
    }

    // This event's id.
    public UUID getId() {
        return id;
    }

    // The domain event type.
    public String getEventType() {
        return eventType;
    }

    // The serialized event payload.
    public String getPayload() {
        return payload;
    }

    // Entity equality by id.
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof OutboxEvent that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    // Hash consistent with id-based equality.
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
