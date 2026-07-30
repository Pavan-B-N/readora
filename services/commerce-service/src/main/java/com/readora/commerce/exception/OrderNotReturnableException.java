package com.readora.commerce.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

// Thrown when a return is attempted on an order outside its return window or not yet delivered.
public class OrderNotReturnableException extends ServiceException {
    // Builds the 409 CONFLICT response.
    public OrderNotReturnableException() {
        super(
                "ORDER_NOT_RETURNABLE", HttpStatus.CONFLICT,
                "This order isn't eligible for return — it must be delivered and within the 2-day return window"
        );
    }
}
