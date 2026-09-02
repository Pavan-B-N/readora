package com.readora.commerce.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

public class StoreIdRequiredException extends ServiceException {
    public StoreIdRequiredException() {
        super("STORE_ID_REQUIRED", HttpStatus.BAD_REQUEST,
                "storeId is required to add a physical book to the cart");
    }
}
