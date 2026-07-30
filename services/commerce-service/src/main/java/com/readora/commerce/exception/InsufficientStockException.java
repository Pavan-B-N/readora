package com.readora.commerce.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

// Thrown when a physical item's stock can't cover the requested quantity.
public class InsufficientStockException extends ServiceException {
    // Builds the 409 CONFLICT response with a caller-supplied detail message.
    public InsufficientStockException(String message) {
        super("INSUFFICIENT_STOCK", HttpStatus.CONFLICT, message);
    }
}
