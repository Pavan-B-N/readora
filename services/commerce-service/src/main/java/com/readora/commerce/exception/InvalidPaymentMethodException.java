package com.readora.commerce.exception;

import org.springframework.http.HttpStatus;

public class InvalidPaymentMethodException extends ServiceException {
    public InvalidPaymentMethodException(String message) {
        super("INVALID_PAYMENT_METHOD", HttpStatus.BAD_REQUEST, message);
    }
}
