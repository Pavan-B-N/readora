package com.readora.delivery.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

// No delivery assignment exists for the given lookup.
public class AssignmentNotFoundException extends ServiceException {
    // Builds the 404 response for a missing assignment.
    public AssignmentNotFoundException() {
        super("ASSIGNMENT_NOT_FOUND", HttpStatus.NOT_FOUND, "No such delivery assignment");
    }
}
