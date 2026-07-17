package com.readora.auth.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

/** Thrown at registration when an account already exists for the requested email. */
public class EmailAlreadyRegisteredException extends ServiceException {

    // Creates the exception with a message naming the conflicting email.
    public EmailAlreadyRegisteredException(String email) {
        super("EMAIL_ALREADY_REGISTERED", HttpStatus.CONFLICT,
                "An account already exists for email " + email);
    }
}
