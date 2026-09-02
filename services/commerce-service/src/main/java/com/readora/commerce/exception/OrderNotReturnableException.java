package com.readora.commerce.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

public class OrderNotReturnableException extends ServiceException {
    public OrderNotReturnableException() {
        super(
                "ORDER_NOT_RETURNABLE", HttpStatus.CONFLICT,
                "This order isn't eligible for return — it must be delivered and within the 2-day return window"
        );
    }
}
