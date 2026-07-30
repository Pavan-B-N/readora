package com.readora.commerce.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

// Thrown when a cancel is attempted more than 48 hours after the order was placed.
public class OrderCancelWindowExpiredException extends ServiceException {
    // Builds the 409 CONFLICT response.
    public OrderCancelWindowExpiredException() {
        super("ORDER_CANCEL_WINDOW_EXPIRED", HttpStatus.CONFLICT, "Orders can only be cancelled within 48 hours of being placed");
    }
}
