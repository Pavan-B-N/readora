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

    /**
     * Creates a new role.
     *
     * @param code        the role's unique code
     * @param description a human-readable description of the role
     */
    public Role(RoleCode code, String description) {
        this.code = code;
        this.description = description;
    }

    /** @return the role's primary key */
    public UUID getId() {
        return id;
    }

    /** @return the role's unique code */
    public RoleCode getCode() {
        return code;
    }

    /** @return a human-readable description of the role */
    public String getDescription() {
        return description;
    }

    /** @param description the new description to set */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Equality is id-based only, and only for persisted entities.
     *
     * @param obj the object to compare against
     * @return true if obj is a Role with the same non-null id
     */
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

    /** @return a hash code consistent with the id-based equals() implementation */
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
