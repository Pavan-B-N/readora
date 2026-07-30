package com.readora.commerce.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

// Thrown when a PHYSICAL cart item is added without a storeId.
public class StoreIdRequiredException extends ServiceException {
    // Builds the 400 BAD_REQUEST response.
    public StoreIdRequiredException() {
        super("STORE_ID_REQUIRED", HttpStatus.BAD_REQUEST,
                "storeId is required to add a physical book to the cart");
    }
}
