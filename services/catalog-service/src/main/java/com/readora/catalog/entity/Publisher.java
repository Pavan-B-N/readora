package com.readora.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;
import java.util.UUID;

// JPA entity for a book publisher.
@Entity
@Table(name = "publishers", schema = "catalog")
public class Publisher {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "slug", nullable = false, unique = true)
    private String slug;

    // No-arg constructor required by JPA.
    protected Publisher() {
    }

    // Creates a new publisher with the given details.
    public Publisher(String name, String slug) {
        this.name = name;
        this.slug = slug;
    }

    // Returns the publisher's id.
    public UUID getId() {
        return id;
    }

    // Returns the publisher's name.
    public String getName() {
        return name;
    }

    // Returns the publisher's URL slug.
    public String getSlug() {
        return slug;
    }

    // Entities are equal when they share a non-null id.
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Publisher publisher)) return false;
        return id != null && Objects.equals(id, publisher.id);
    }

    // Hashes by id, consistent with equals.
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
