package com.readora.catalog.exception;

import org.springframework.http.HttpStatus;

public class AdminStoreAccessDeniedException extends ServiceException {
    public AdminStoreAccessDeniedException() {
        super("ADMIN_STORE_ACCESS_DENIED", HttpStatus.FORBIDDEN, "You can only list books under your own assigned store");
    }
}
