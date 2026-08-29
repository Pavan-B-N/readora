package com.readora.commerce.exception;

import org.springframework.http.HttpStatus;

public class StoreNotFoundException extends ServiceException {
    public StoreNotFoundException() {
        super("STORE_NOT_FOUND", HttpStatus.NOT_FOUND, "No such store");
    }
}
