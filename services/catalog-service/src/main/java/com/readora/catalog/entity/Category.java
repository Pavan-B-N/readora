package com.readora.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;
import java.util.UUID;

/** Flat — deliberately 1D, no nesting (e.g. Technology, Science, Business, Biology). */
@Entity
@Table(name = "categories", schema = "catalog")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "slug", nullable = false, unique = true)
    private String slug;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    // No-arg constructor required by JPA.
    protected Category() {
    }

    // Creates a new category with the given details.
    public Category(String name, String slug, int displayOrder) {
        this.name = name;
        this.slug = slug;
        this.displayOrder = displayOrder;
    }

    // Overwrites this category's mutable fields in place.
    public void update(String name, String slug, int displayOrder) {
        this.name = name;
        this.slug = slug;
        this.displayOrder = displayOrder;
    }

    // Returns the category's id.
    public UUID getId() {
        return id;
    }

    // Returns the category's name.
    public String getName() {
        return name;
    }

    // Returns the category's URL slug.
    public String getSlug() {
        return slug;
    }

    // Returns the category's display order.
    public int getDisplayOrder() {
        return displayOrder;
    }

    // Entities are equal when they share a non-null id.
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Category category)) return false;
        return id != null && Objects.equals(id, category.id);
    }

    // Hashes by id, consistent with equals.
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
