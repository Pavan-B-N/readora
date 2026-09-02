package com.readora.auth.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

/** Thrown at /auth/refresh when the presented token is unknown, expired, or already revoked. */
public class RefreshTokenInvalidException extends ServiceException {

    /** Creates the exception with a fixed message. */
    public RefreshTokenInvalidException() {
        super("REFRESH_TOKEN_INVALID", HttpStatus.UNAUTHORIZED,
                "Refresh token is unknown, expired, or already revoked");
    }
}
