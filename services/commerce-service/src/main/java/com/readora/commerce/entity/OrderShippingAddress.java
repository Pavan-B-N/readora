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

/**
 * Immutable copy of the address used at checkout, taken directly from the checkout request
 * rather than looked up from user-service by addressId (see build notes for why). order_id is
 * both PK and FK via @MapsId — a shared primary key, not a plain cross-service UUID reference.
 */
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

    protected OrderShippingAddress() {
    }

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

    public String getRecipientName() {
        return recipientName;
    }

    public String getLine1() {
        return line1;
    }

    public String getLine2() {
        return line2;
    }

    public String getCity() {
        return city;
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

    public String getPhone() {
        return phone;
    }
}
