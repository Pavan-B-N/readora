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

/** One message in the small admin<->customer chat that opens while a return sits at RETURN_REQUESTED. */
@Entity
@Table(name = "return_messages", schema = "commerce")
public class ReturnMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "sender_user_id", nullable = false)
    private UUID senderUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_role", nullable = false)
    private ReturnSenderRole senderRole;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // JPA no-arg constructor.
    protected ReturnMessage() {
    }

    // Creates a return-chat message from the given sender.
    public ReturnMessage(Order order, UUID senderUserId, ReturnSenderRole senderRole, String content) {
        this.order = order;
        this.senderUserId = senderUserId;
        this.senderRole = senderRole;
        this.content = content;
    }

    // Stamps the send time before insert.
    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    // This message's id.
    public UUID getId() {
        return id;
    }

    // The order this message belongs to.
    public Order getOrder() {
        return order;
    }

    // The user id of the sender.
    public UUID getSenderUserId() {
        return senderUserId;
    }

    // Whether the sender was the customer or an admin.
    public ReturnSenderRole getSenderRole() {
        return senderRole;
    }

    // The message text.
    public String getContent() {
        return content;
    }

    // When the message was sent.
    public Instant getCreatedAt() {
        return createdAt;
    }

    // Entity equality by id.
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ReturnMessage that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    // Hash consistent with id-based equality.
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
