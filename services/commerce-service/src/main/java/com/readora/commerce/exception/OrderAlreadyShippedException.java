package com.readora.commerce.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

// Thrown when a cancel is attempted on an order that has already shipped.
public class OrderAlreadyShippedException extends ServiceException {
    // Builds the 409 CONFLICT response.
    public OrderAlreadyShippedException() {
        super("ORDER_ALREADY_SHIPPED", HttpStatus.CONFLICT, "The order has already shipped and can no longer be cancelled");
    }
}
