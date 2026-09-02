package com.readora.catalog.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

public class StoreIdRequiredException extends ServiceException {
    public StoreIdRequiredException() {
        super("STORE_ID_REQUIRED", HttpStatus.BAD_REQUEST,
                "storeId is required for the physical catalogue — a customer shops one store at a time");
    }
}
