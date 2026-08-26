package com.readora.commerce.exception;

import org.springframework.http.HttpStatus;

public class OrderNotReturnableException extends ServiceException {
    public OrderNotReturnableException() {
        super(
                "ORDER_NOT_RETURNABLE", HttpStatus.CONFLICT,
                "This order isn't eligible for return — it must be delivered and within the 7-day return window"
        );
    }
}
