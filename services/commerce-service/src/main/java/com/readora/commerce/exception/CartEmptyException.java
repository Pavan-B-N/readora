package com.readora.commerce.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

public class CartEmptyException extends ServiceException {
    public CartEmptyException() {
        super("CART_EMPTY", HttpStatus.CONFLICT, "Nothing to check out");
    }
}
