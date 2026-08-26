package com.readora.commerce.exception;

import org.springframework.http.HttpStatus;

public class AdminStoreNotAssignedException extends ServiceException {
    public AdminStoreNotAssignedException() {
        super("ADMIN_STORE_NOT_ASSIGNED", HttpStatus.FORBIDDEN, "Your account isn't assigned to a store yet");
    }
}
