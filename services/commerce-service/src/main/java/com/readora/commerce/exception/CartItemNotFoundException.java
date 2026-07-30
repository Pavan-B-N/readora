package com.readora.commerce.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

// Thrown when a referenced cart item doesn't exist in the caller's cart.
public class CartItemNotFoundException extends ServiceException {
    // Builds the 404 NOT_FOUND response.
    public CartItemNotFoundException() {
        super("CART_ITEM_NOT_FOUND", HttpStatus.NOT_FOUND, "That book is not in the cart");
    }
}
