package com.readora.commerce.exception;

import org.springframework.http.HttpStatus;

/**
 * Physical browsing is store-scoped per customer, so this shouldn't happen in practice — a
 * defensive guard against a stale cart spanning two stores (e.g. the customer switched stores
 * mid-session without the cart being cleared).
 */
public class MultipleStoresInCartException extends ServiceException {
    public MultipleStoresInCartException() {
        super(
                "MULTIPLE_STORES_IN_CART", HttpStatus.CONFLICT,
                "Physical items in this order come from more than one store — clear your cart and add items from a single store"
        );
    }
}
