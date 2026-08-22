package com.readora.catalog.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class InsufficientStockException extends ServiceException {
    public InsufficientStockException(UUID bookId) {
        super("INSUFFICIENT_STOCK", HttpStatus.CONFLICT, "Requested quantity exceeds available inventory for book " + bookId);
    }
}
