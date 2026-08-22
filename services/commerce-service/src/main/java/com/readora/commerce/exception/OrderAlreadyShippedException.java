package com.readora.commerce.exception;

import org.springframework.http.HttpStatus;

public class OrderAlreadyShippedException extends ServiceException {
    public OrderAlreadyShippedException() {
        super("ORDER_ALREADY_SHIPPED", HttpStatus.CONFLICT, "The order has already shipped and can no longer be cancelled");
    }
}
