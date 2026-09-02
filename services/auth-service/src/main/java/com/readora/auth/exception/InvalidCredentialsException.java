package com.readora.auth.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

/** Thrown at login for either an unknown email or a wrong password — deliberately the same for both, so the endpoint can't be used to enumerate registered accounts. */
public class InvalidCredentialsException extends ServiceException {

    /** Creates the exception with a fixed, enumeration-safe message. */
    public InvalidCredentialsException() {
        super("INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED,
                "Email or password is incorrect");
    }
}
