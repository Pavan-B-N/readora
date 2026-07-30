package com.readora.commerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.util.UUID;

// Immutable copy of the address used at checkout, taken directly from the checkout request rather than looked up from user-service by addressId; order_id is both PK and FK via @MapsId, a shared primary key rather than a plain cross-service UUID reference.
@Entity
@Table(name = "order_shipping_addresses", schema = "commerce")
public class OrderShippingAddress {

    @Id
    @Column(name = "order_id")
    private UUID orderId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "order_id")
    private Order order;

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

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "phone")
    private String phone;

    // JPA no-arg constructor.
    protected OrderShippingAddress() {
    }

    // Builds the address snapshot for an order at checkout time.
    public OrderShippingAddress(
            Order order, String recipientName, String line1, String line2, String city,
            String state, String postalCode, String countryCode, String phone
    ) {
        this.order = order;
        this.recipientName = recipientName;
        this.line1 = line1;
        this.line2 = line2;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.countryCode = countryCode;
        this.phone = phone;
    }

    // Recipient's name on the shipment.
    public String getRecipientName() {
        return recipientName;
    }

    // First line of the address.
    public String getLine1() {
        return line1;
    }

    // Second line of the address, if any.
    public String getLine2() {
        return line2;
    }

    // City of the address.
    public String getCity() {
        return city;
    }

    // State/province of the address.
    public String getState() {
        return state;
    }

    // Postal/ZIP code of the address.
    public String getPostalCode() {
        return postalCode;
    }

    // ISO country code of the address.
    public String getCountryCode() {
        return countryCode;
    }

    // Contact phone number for the delivery.
    public String getPhone() {
        return phone;
    }
}
