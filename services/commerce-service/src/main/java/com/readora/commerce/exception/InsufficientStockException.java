package com.readora.commerce.exception;

import org.springframework.http.HttpStatus;

public class InsufficientStockException extends ServiceException {
    public InsufficientStockException(String message) {
        super("INSUFFICIENT_STOCK", HttpStatus.CONFLICT, message);
    }
}
