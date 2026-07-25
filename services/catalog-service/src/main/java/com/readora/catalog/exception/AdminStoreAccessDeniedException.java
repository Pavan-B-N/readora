package com.readora.catalog.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

// Thrown when an admin tries to access books belonging to a store other than the one they're assigned to.
public class AdminStoreAccessDeniedException extends ServiceException {
    public AdminStoreAccessDeniedException() {
        super("ADMIN_STORE_ACCESS_DENIED", HttpStatus.FORBIDDEN, "You can only list books under your own assigned store");
    }
}
