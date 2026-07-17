package com.readora.auth.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

/** Thrown at login when the account has been locked after too many consecutive failed attempts. */
public class AccountLockedException extends ServiceException {

    /** Creates the exception with a fixed message. */
    public AccountLockedException() {
        super("ACCOUNT_LOCKED", HttpStatus.LOCKED,
                "Account is locked due to too many failed login attempts");
    }
}
