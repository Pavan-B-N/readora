package com.readora.commerce.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

// Thrown when the requested payment method is missing or unsupported.
public class InvalidPaymentMethodException extends ServiceException {
    // Builds the 400 BAD_REQUEST response with a caller-supplied detail message.
    public InvalidPaymentMethodException(String message) {
        super("INVALID_PAYMENT_METHOD", HttpStatus.BAD_REQUEST, message);
    }
}
