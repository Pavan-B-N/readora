package com.readora.auth.exception;

import org.springframework.http.HttpStatus;

public class EmailAlreadyRegisteredException extends AuthException {

    public EmailAlreadyRegisteredException(String email) {
        super("EMAIL_ALREADY_REGISTERED", HttpStatus.CONFLICT,
                "An account already exists for email " + email);
    }
}
