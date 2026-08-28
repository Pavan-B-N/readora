package com.readora.delivery.exception;

import org.springframework.http.HttpStatus;

public class InvalidReturnPickupTransitionException extends ServiceException {
    public InvalidReturnPickupTransitionException() {
        super("INVALID_RETURN_PICKUP_TRANSITION", HttpStatus.CONFLICT, "That action can't be applied from the pickup's current status");
    }
}
