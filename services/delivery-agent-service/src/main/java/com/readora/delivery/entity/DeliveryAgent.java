package com.readora.delivery.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * user_id is a cross-service reference to auth.User — plain UUID, never a JPA relationship,
 * since that entity lives in a different service entirely (same convention as
 * user-service's UserProfile). storeId is likewise a cross-service reference to catalog.Store.
 */
@Entity
@Table(name = "delivery_agents", schema = "delivery")
public class DeliveryAgent {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "phone")
    private String phone;

    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected DeliveryAgent() {
    }

    public DeliveryAgent(UUID userId, String name, String phone, UUID storeId) {
        this.userId = userId;
        this.name = name;
        this.phone = phone;
        this.storeId = storeId;
    }

    @jakarta.persistence.PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public UUID getStoreId() {
        return storeId;
    }

    public boolean isActive() {
        return isActive;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof DeliveryAgent agent)) return false;
        return userId != null && Objects.equals(userId, agent.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(userId);
    }
}
