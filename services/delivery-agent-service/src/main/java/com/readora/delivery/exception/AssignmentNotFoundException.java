package com.readora.delivery.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

public class AssignmentNotFoundException extends ServiceException {
    public AssignmentNotFoundException() {
        super("ASSIGNMENT_NOT_FOUND", HttpStatus.NOT_FOUND, "No such delivery assignment");
    }
}
