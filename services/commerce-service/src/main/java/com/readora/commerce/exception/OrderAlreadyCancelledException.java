package com.readora.commerce.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

// Thrown when a cancel is attempted on an order that's already cancelled.
public class OrderAlreadyCancelledException extends ServiceException {
    // Builds the 409 CONFLICT response.
    public OrderAlreadyCancelledException() {
        super("ORDER_ALREADY_CANCELLED", HttpStatus.CONFLICT, "The order is already in CANCELLED");
    }
}
