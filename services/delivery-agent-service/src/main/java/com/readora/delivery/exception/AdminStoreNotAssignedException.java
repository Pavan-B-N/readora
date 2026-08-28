package com.readora.delivery.exception;

import org.springframework.http.HttpStatus;

public class AdminStoreNotAssignedException extends ServiceException {
    public AdminStoreNotAssignedException() {
        super("ADMIN_STORE_NOT_ASSIGNED", HttpStatus.FORBIDDEN, "Your admin account has no store assigned");
    }
}
