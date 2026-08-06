package com.readora.delivery.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

// The requested status change isn't legal from the pickup's current status.
public class InvalidReturnPickupTransitionException extends ServiceException {
    // Builds the 409 response for an illegal status transition.
    public InvalidReturnPickupTransitionException() {
        super("INVALID_RETURN_PICKUP_TRANSITION", HttpStatus.CONFLICT, "That action can't be applied from the pickup's current status");
    }
}
