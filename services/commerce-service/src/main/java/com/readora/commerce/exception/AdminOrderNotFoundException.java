package com.readora.commerce.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

// Thrown when an admin looks up an order that doesn't exist at their store.
public class AdminOrderNotFoundException extends ServiceException {
    // Builds the 404 NOT_FOUND response.
    public AdminOrderNotFoundException() {
        super("ORDER_NOT_FOUND", HttpStatus.NOT_FOUND, "No such order at your store");
    }
}
