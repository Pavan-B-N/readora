package com.readora.commerce.entity;

// Whether a book is fulfilled physically (shipped from the store) or virtually (instant digital delivery); chosen per cart/order item, so a cart can mix both, and a shipping address is only required when at least one item is PHYSICAL.
public enum DeliveryType {
    PHYSICAL,
    VIRTUAL
}
