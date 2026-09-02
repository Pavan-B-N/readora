package com.readora.delivery.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

/** Two agents raced to claim the same order — whoever loses gets this. */
public class AssignmentAlreadyClaimedException extends ServiceException {
    public AssignmentAlreadyClaimedException() {
        super("ASSIGNMENT_ALREADY_CLAIMED", HttpStatus.CONFLICT, "Another agent already claimed this order");
    }
}
