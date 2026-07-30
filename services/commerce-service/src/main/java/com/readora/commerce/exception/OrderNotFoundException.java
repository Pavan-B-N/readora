package com.readora.commerce.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

// Thrown when a referenced order doesn't exist, or belongs to another user.
public class OrderNotFoundException extends ServiceException {
    // Builds the 404 NOT_FOUND response.
    public OrderNotFoundException() {
        super("ORDER_NOT_FOUND", HttpStatus.NOT_FOUND, "No such order, or it belongs to another user");
    }
}
