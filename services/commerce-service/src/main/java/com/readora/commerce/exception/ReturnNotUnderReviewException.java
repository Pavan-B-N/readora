package com.readora.commerce.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

/** Thrown when an admin tries to approve/reject a return, or post a chat message, that isn't awaiting review. */
public class ReturnNotUnderReviewException extends ServiceException {
    public ReturnNotUnderReviewException() {
        super(
                "RETURN_NOT_UNDER_REVIEW", HttpStatus.CONFLICT,
                "This return isn't awaiting review — it may already have been decided"
        );
    }
}
