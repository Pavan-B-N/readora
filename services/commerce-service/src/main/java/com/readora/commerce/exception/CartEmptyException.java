package com.readora.commerce.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

// Thrown when checkout is attempted with an empty item list.
public class CartEmptyException extends ServiceException {
    // Builds the 409 CONFLICT response.
    public CartEmptyException() {
        super("CART_EMPTY", HttpStatus.CONFLICT, "Nothing to check out");
    }
}
