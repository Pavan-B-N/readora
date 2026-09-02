package com.readora.commerce.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

/** Thrown when delivery-agent-service requests a delivery-status transition out of sequence. */
public class InvalidDeliveryTransitionException extends ServiceException {
    public InvalidDeliveryTransitionException() {
        super(
                "INVALID_DELIVERY_TRANSITION", HttpStatus.CONFLICT,
                "That delivery status can't be applied from the order's current status"
        );
    }
}
