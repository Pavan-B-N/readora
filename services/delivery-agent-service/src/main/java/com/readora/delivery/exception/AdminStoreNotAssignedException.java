package com.readora.delivery.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

// The caller is an admin but has no store scoped to their account.
public class AdminStoreNotAssignedException extends ServiceException {
    // Builds the 403 response for a storeless admin.
    public AdminStoreNotAssignedException() {
        super("ADMIN_STORE_NOT_ASSIGNED", HttpStatus.FORBIDDEN, "Your admin account has no store assigned");
    }
}
