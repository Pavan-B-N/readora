package com.readora.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;
import java.util.UUID;

// A physical fulfilment location — every book belongs to exactly one store, and a customer shops one store at a time (quick-commerce: everything in a cart ships from the same store); only Bangalore is seeded today, but the model supports more.
@Entity
@Table(name = "stores", schema = "catalog")
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "city", nullable = false)
    private String city;

    @Column(name = "line1", nullable = false)
    private String line1;

    @Column(name = "line2")
    private String line2;

    @Column(name = "state", nullable = false)
    private String state;

    @Column(name = "postal_code", nullable = false)
    private String postalCode;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    // No-arg constructor required by JPA.
    protected Store() {
    }

    // Creates a new active store with the given details.
    public Store(String name, String city, String line1, String line2, String state, String postalCode, String countryCode) {
        this.name = name;
        this.city = city;
        this.line1 = line1;
        this.line2 = line2;
        this.state = state;
        this.postalCode = postalCode;
        this.countryCode = countryCode;
    }

    // Returns the store's id.
    public UUID getId() {
        return id;
    }

    // Returns the store's name.
    public String getName() {
        return name;
    }

    // Returns the store's city.
    public String getCity() {
        return city;
    }

    // Returns the first address line.
    public String getLine1() {
        return line1;
    }

    // Returns the second address line, if any.
    public String getLine2() {
        return line2;
    }

    // Returns the store's state/region.
    public String getState() {
        return state;
    }

    // Returns the store's postal code.
    public String getPostalCode() {
        return postalCode;
    }

    // Returns the store's ISO country code.
    public String getCountryCode() {
        return countryCode;
    }

    // Whether the store is currently active.
    public boolean isActive() {
        return isActive;
    }

    // Entities are equal when they share a non-null id.
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Store store)) return false;
        return id != null && Objects.equals(id, store.id);
    }

    // Hashes by id, consistent with equals.
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
