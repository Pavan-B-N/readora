package com.readora.commerce.exception;

import org.springframework.http.HttpStatus;

public class CartItemNotFoundException extends ServiceException {
    public CartItemNotFoundException() {
        super("CART_ITEM_NOT_FOUND", HttpStatus.NOT_FOUND, "That book is not in the cart");
    }
}
