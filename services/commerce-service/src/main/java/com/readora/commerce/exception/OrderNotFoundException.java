package com.readora.commerce.exception;

import org.springframework.http.HttpStatus;

public class OrderNotFoundException extends ServiceException {
    public OrderNotFoundException() {
        super("ORDER_NOT_FOUND", HttpStatus.NOT_FOUND, "No such order, or it belongs to another user");
    }
}
