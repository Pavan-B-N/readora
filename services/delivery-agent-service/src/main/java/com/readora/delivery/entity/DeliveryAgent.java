package com.readora.delivery.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

// user_id and storeId are plain UUID cross-service references (to auth.User and catalog.Store), never JPA relationships, since those entities live in other services — same convention as user-service's UserProfile.
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

    // Whether the agent is clocked in now, distinct from isActive ("real, enabled account"); off-duty agents don't see new orders but keep access to what they already claimed, and columnDefinition gives the ALTER TABLE a DEFAULT so this NOT NULL column can be added to the already-seeded table (ddl-auto: update, no migration tooling).
    @Column(name = "on_duty", nullable = false, columnDefinition = "boolean not null default false")
    private boolean onDuty = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // No-arg constructor required by JPA.
    protected DeliveryAgent() {
    }

    // Creates a new agent for the given user and store.
    public DeliveryAgent(UUID userId, String name, String phone, UUID storeId) {
        this.userId = userId;
        this.name = name;
        this.phone = phone;
        this.storeId = storeId;
    }

    // Stamps createdAt right before the row is inserted.
    @jakarta.persistence.PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    // Toggles on-duty status.
    public void setOnDuty(boolean onDuty) {
        this.onDuty = onDuty;
    }

    // Returns the agent's user id.
    public UUID getUserId() {
        return userId;
    }

    // Returns the agent's name.
    public String getName() {
        return name;
    }

    // Returns the agent's phone number.
    public String getPhone() {
        return phone;
    }

    // Returns the id of the store this agent belongs to.
    public UUID getStoreId() {
        return storeId;
    }

    // Whether this is an enabled account.
    public boolean isActive() {
        return isActive;
    }

    // Whether the agent is currently clocked in.
    public boolean isOnDuty() {
        return onDuty;
    }

    // Returns when the agent record was created.
    public Instant getCreatedAt() {
        return createdAt;
    }

    // Entity equality by user id.
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof DeliveryAgent agent)) return false;
        return userId != null && Objects.equals(userId, agent.userId);
    }

    // Hash based on user id.
    @Override
    public int hashCode() {
        return Objects.hashCode(userId);
    }
}
