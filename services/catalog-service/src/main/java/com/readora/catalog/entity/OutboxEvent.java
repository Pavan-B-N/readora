package com.readora.catalog.entity;

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
@Table(name = "outbox_events", schema = "catalog")
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

    // No-arg constructor required by JPA.
    protected OutboxEvent() {
    }

    // Creates a new pending outbox event for the given aggregate.
    public OutboxEvent(String aggregateType, UUID aggregateId, String eventType, String payload) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
    }

    // Stamps createdAt before insert.
    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    // Marks this event as published, stamping publishedAt with the current time.
    public void markPublished() {
        this.publishedAt = Instant.now();
    }

    // Returns the event's id.
    public UUID getId() {
        return id;
    }

    // Returns the event type.
    public String getEventType() {
        return eventType;
    }

    // Returns the serialized event payload.
    public String getPayload() {
        return payload;
    }

    // Entities are equal when they share a non-null id.
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof OutboxEvent that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    // Hashes by id, consistent with equals.
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
