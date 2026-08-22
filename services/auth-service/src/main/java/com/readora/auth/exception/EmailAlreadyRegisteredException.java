package com.readora.auth.exception;

import org.springframework.http.HttpStatus;

/** Thrown at registration when an account already exists for the requested email. */
public class EmailAlreadyRegisteredException extends AuthException {

    /** @param email the email address that is already registered */
    public EmailAlreadyRegisteredException(String email) {
        super("EMAIL_ALREADY_REGISTERED", HttpStatus.CONFLICT,
                "An account already exists for email " + email);
    }
}
