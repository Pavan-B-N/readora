package com.readora.delivery.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

// The requested status change isn't legal from the assignment's current status.
public class InvalidAssignmentTransitionException extends ServiceException {
    // Builds the 409 response for an illegal status transition.
    public InvalidAssignmentTransitionException() {
        super("INVALID_ASSIGNMENT_TRANSITION", HttpStatus.CONFLICT, "That action can't be applied from the assignment's current status");
    }
}
