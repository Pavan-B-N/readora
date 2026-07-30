package com.readora.commerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

// One recorded status transition for an order, forming its audit trail.
@Entity
@Table(name = "order_status_history", schema = "commerce")
public class OrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status")
    private OrderStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false)
    private OrderStatus toStatus;

    @Column(name = "reason")
    private String reason;

    @Column(name = "changed_by")
    private String changedBy;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    // JPA no-arg constructor.
    protected OrderStatusHistory() {
    }

    // Records a status transition for the given order.
    public OrderStatusHistory(Order order, OrderStatus fromStatus, OrderStatus toStatus, String reason, String changedBy) {
        this.order = order;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.reason = reason;
        this.changedBy = changedBy;
    }

    // Stamps the transition time before insert.
    @PrePersist
    protected void onCreate() {
        this.changedAt = Instant.now();
    }

    // The status this transition moved the order into.
    public OrderStatus getToStatus() {
        return toStatus;
    }

    // When this transition happened.
    public Instant getChangedAt() {
        return changedAt;
    }

    // Entity equality by id.
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof OrderStatusHistory that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    // Hash consistent with id-based equality.
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
