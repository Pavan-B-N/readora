package com.readora.commerce.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

// Thrown when a cart/checkout requests more than 10 units of a single title.
public class QtyLimitExceededException extends ServiceException {
    // Builds the 422 UNPROCESSABLE_ENTITY response.
    public QtyLimitExceededException() {
        super("QTY_LIMIT_EXCEEDED", HttpStatus.UNPROCESSABLE_ENTITY, "More than 10 of a single title");
    }
}
