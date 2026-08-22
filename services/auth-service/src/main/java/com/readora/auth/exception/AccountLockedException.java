package com.readora.auth.exception;

import org.springframework.http.HttpStatus;

public class AccountLockedException extends AuthException {

    public AccountLockedException() {
        super("ACCOUNT_LOCKED", HttpStatus.LOCKED,
                "Account is locked due to too many failed login attempts");
    }
}
