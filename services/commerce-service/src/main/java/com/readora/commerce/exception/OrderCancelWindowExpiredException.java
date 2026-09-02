package com.readora.commerce.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

public class OrderCancelWindowExpiredException extends ServiceException {
    public OrderCancelWindowExpiredException() {
        super("ORDER_CANCEL_WINDOW_EXPIRED", HttpStatus.CONFLICT, "Orders can only be cancelled within 48 hours of being placed");
    }
}
