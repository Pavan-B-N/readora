package com.readora.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

// A saved delivery address belonging to a user.
@Entity
@Table(name = "addresses", schema = "users")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "label", nullable = false)
    private AddressLabel label;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_type", nullable = false)
    private AddressRecipientType recipientType;

    @Column(name = "recipient_name", nullable = false)
    private String recipientName;

    @Column(name = "line1", nullable = false)
    private String line1;

    @Column(name = "line2")
    private String line2;

    @Column(name = "city", nullable = false)
    private String city;

    @Column(name = "state", nullable = false)
    private String state;

    @Column(name = "postal_code", nullable = false)
    private String postalCode;

    @Column(name = "country_code", nullable = false)
    private String countryCode;

    /** The store whose service area this address falls in — quick-commerce delivers from one store. */
    @Column(name = "store_id")
    private UUID storeId;

    @Column(name = "phone")
    private String phone;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    // JPA no-arg constructor.
    protected Address() {
    }

    // Creates a new address for a user.
    public Address(
            UUID userId, AddressLabel label, AddressRecipientType recipientType, String recipientName,
            String line1, String line2, String city, String state, String postalCode, String countryCode,
            UUID storeId, String phone, boolean isDefault
    ) {
        this.userId = userId;
        this.label = label;
        this.recipientType = recipientType;
        this.recipientName = recipientName;
        this.line1 = line1;
        this.line2 = line2;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.countryCode = countryCode;
        this.storeId = storeId;
        this.phone = phone;
        this.isDefault = isDefault;
    }

    // Marks the address as deleted without removing the row.
    public void softDelete() {
        this.deletedAt = Instant.now();
    }

    // Clears the default flag.
    public void clearDefault() {
        this.isDefault = false;
    }

    // Sets this address as the default.
    public void markDefault() {
        this.isDefault = true;
    }

    // Address id.
    public UUID getId() {
        return id;
    }

    // Owning user id.
    public UUID getUserId() {
        return userId;
    }

    // Address label (home/work/other).
    public AddressLabel getLabel() {
        return label;
    }

    // Whether the recipient is the account owner or a guest.
    public AddressRecipientType getRecipientType() {
        return recipientType;
    }

    // Recipient's name.
    public String getRecipientName() {
        return recipientName;
    }

    // First line of the address.
    public String getLine1() {
        return line1;
    }

    // Second line of the address.
    public String getLine2() {
        return line2;
    }

    // City.
    public String getCity() {
        return city;
    }

    // State.
    public String getState() {
        return state;
    }

    // Postal code.
    public String getPostalCode() {
        return postalCode;
    }

    // ISO country code.
    public String getCountryCode() {
        return countryCode;
    }

    // The store whose service area this address falls in.
    public UUID getStoreId() {
        return storeId;
    }

    // Recipient's phone number.
    public String getPhone() {
        return phone;
    }

    // Whether this is the user's default address.
    public boolean isDefault() {
        return isDefault;
    }

    // When the address was soft-deleted, if at all.
    public Instant getDeletedAt() {
        return deletedAt;
    }

    // Entity equality by id.
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Address address)) return false;
        return id != null && Objects.equals(id, address.id);
    }

    // Entity hash by id.
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
