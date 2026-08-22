package com.readora.commerce.exception;

import org.springframework.http.HttpStatus;

public class CartEmptyException extends ServiceException {
    public CartEmptyException() {
        super("CART_EMPTY", HttpStatus.CONFLICT, "Nothing to check out");
    }
}
