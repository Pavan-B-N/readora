package com.readora.commerce.exception;

import org.springframework.http.HttpStatus;

public class AdminOrderNotFoundException extends ServiceException {
    public AdminOrderNotFoundException() {
        super("ORDER_NOT_FOUND", HttpStatus.NOT_FOUND, "No such order at your store");
    }
}
