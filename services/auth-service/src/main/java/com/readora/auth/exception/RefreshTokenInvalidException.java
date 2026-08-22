package com.readora.auth.exception;

import org.springframework.http.HttpStatus;

public class RefreshTokenInvalidException extends AuthException {

    public RefreshTokenInvalidException() {
        super("REFRESH_TOKEN_INVALID", HttpStatus.UNAUTHORIZED,
                "Refresh token is unknown, expired, or already revoked");
    }
}
