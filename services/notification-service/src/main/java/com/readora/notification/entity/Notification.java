package com.readora.notification.entity;

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

/** A persisted notification — pushed live over WebSocket when created, and listable/markable-read afterward. */
@Entity
@Table(name = "notifications", schema = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "read", nullable = false)
    private boolean read = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // JPA-only no-arg constructor.
    protected Notification() {
    }

    // Builds a new, unread notification for a user.
    public Notification(UUID userId, String type, String title, String message, UUID orderId) {
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.orderId = orderId;
    }

    // Stamps the creation time just before the row is inserted.
    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    // Flips this notification to read.
    public void markRead() {
        this.read = true;
    }

    // Returns the notification's id.
    public UUID getId() {
        return id;
    }

    // Returns the owning user's id.
    public UUID getUserId() {
        return userId;
    }

    // Returns the notification's type code.
    public String getType() {
        return type;
    }

    // Returns the notification's title.
    public String getTitle() {
        return title;
    }

    // Returns the notification's body message.
    public String getMessage() {
        return message;
    }

    // Returns the related order id, if any.
    public UUID getOrderId() {
        return orderId;
    }

    // Whether the notification has been read.
    public boolean isRead() {
        return read;
    }

    // Returns when the notification was created.
    public Instant getCreatedAt() {
        return createdAt;
    }

    // Entities are equal when they share a persisted id.
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Notification that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    // Consistent with the id-based equals().
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
