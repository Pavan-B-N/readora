package com.readora.commerce.exception;

import org.springframework.http.HttpStatus;

public class OrderAlreadyCancelledException extends ServiceException {
    public OrderAlreadyCancelledException() {
        super("ORDER_ALREADY_CANCELLED", HttpStatus.CONFLICT, "The order is already in CANCELLED");
    }
}
