package com.readora.commerce.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

/** Thrown when delivery-agent-service requests a return-pickup status transition out of sequence. */
public class InvalidReturnTransitionException extends ServiceException {
    // Builds the 409 CONFLICT response.
    public InvalidReturnTransitionException() {
        super(
                "INVALID_RETURN_TRANSITION", HttpStatus.CONFLICT,
                "That return status can't be applied from the order's current status"
        );
    }
}
