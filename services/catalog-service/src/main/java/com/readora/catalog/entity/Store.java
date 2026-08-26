package com.readora.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;
import java.util.UUID;

/**
 * A physical fulfilment location. Every book belongs to exactly one store, and a customer
 * shops one store at a time (quick-commerce model — everything in a cart ships from the same
 * store). Only one store (Bangalore) is seeded today; the model supports more.
 */
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

    protected Store() {
    }

    public Store(String name, String city, String line1, String line2, String state, String postalCode, String countryCode) {
        this.name = name;
        this.city = city;
        this.line1 = line1;
        this.line2 = line2;
        this.state = state;
        this.postalCode = postalCode;
        this.countryCode = countryCode;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCity() {
        return city;
    }

    public String getLine1() {
        return line1;
    }

    public String getLine2() {
        return line2;
    }

    public String getState() {
        return state;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public boolean isActive() {
        return isActive;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Store store)) return false;
        return id != null && Objects.equals(id, store.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
