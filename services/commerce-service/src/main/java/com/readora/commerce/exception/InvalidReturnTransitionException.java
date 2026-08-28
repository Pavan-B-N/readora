package com.readora.commerce.exception;

import org.springframework.http.HttpStatus;

/** Thrown when delivery-agent-service requests a return-pickup status transition out of sequence. */
public class InvalidReturnTransitionException extends ServiceException {
    public InvalidReturnTransitionException() {
        super(
                "INVALID_RETURN_TRANSITION", HttpStatus.CONFLICT,
                "That return status can't be applied from the order's current status"
        );
    }
}
