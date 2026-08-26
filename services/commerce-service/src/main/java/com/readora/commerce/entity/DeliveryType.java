package com.readora.commerce.entity;

/**
 * Whether a book is fulfilled physically (shipped from the store) or virtually (instant digital
 * delivery). Chosen per cart item / order item — a cart (and the order it becomes) can mix both;
 * a shipping address is only required when at least one item is PHYSICAL.
 */
public enum DeliveryType {
    PHYSICAL,
    VIRTUAL
}
