package com.readora.delivery.exception;

import org.springframework.http.HttpStatus;

public class InvalidAssignmentTransitionException extends ServiceException {
    public InvalidAssignmentTransitionException() {
        super("INVALID_ASSIGNMENT_TRANSITION", HttpStatus.CONFLICT, "That action can't be applied from the assignment's current status");
    }
}
