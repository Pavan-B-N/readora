package com.readora.commerce.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

// Thrown when a referenced store doesn't exist.
public class StoreNotFoundException extends ServiceException {
    // Builds the 404 NOT_FOUND response.
    public StoreNotFoundException() {
        super("STORE_NOT_FOUND", HttpStatus.NOT_FOUND, "No such store");
    }
}
