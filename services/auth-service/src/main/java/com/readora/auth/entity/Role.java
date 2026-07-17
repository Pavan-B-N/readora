package com.readora.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;
import java.util.UUID;

/** A role that can be assigned to a user (see {@link RoleCode}). */
@Entity
@Table(name = "roles", schema = "auth")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "code", nullable = false, unique = true)
    private RoleCode code;

    @Column(name = "description")
    private String description;

    /** No-arg constructor required by JPA; not for application use. */
    protected Role() {
    }

    // Creates a role with the given code and description.
    public Role(RoleCode code, String description) {
        this.code = code;
        this.description = description;
    }

    // Primary key.
    public UUID getId() {
        return id;
    }

    // The role's code.
    public RoleCode getCode() {
        return code;
    }

    // Human-readable description of the role.
    public String getDescription() {
        return description;
    }

    // Updates the role's description.
    public void setDescription(String description) {
        this.description = description;
    }

    /** Id-based equality, and only for persisted entities. */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Role)) {
            return false;
        }

        Role role = (Role) obj;

        return id != null && Objects.equals(id, role.id);
    }

    // Id-based hash code, consistent with equals().
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
