package com.readora.catalog.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

// Thrown when no store exists with the given id.
public class StoreNotFoundException extends ServiceException {
    public StoreNotFoundException() {
        super("STORE_NOT_FOUND", HttpStatus.NOT_FOUND, "No such store");
    }
}
