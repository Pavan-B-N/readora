package com.readora.commerce.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

// Thrown when a store-admin action is attempted by an admin with no store assigned.
public class AdminStoreNotAssignedException extends ServiceException {
    // Builds the 403 FORBIDDEN response.
    public AdminStoreNotAssignedException() {
        super("ADMIN_STORE_NOT_ASSIGNED", HttpStatus.FORBIDDEN, "Your account isn't assigned to a store yet");
    }
}
