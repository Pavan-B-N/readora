package com.readora.auth.exception;

import org.springframework.http.HttpStatus;

/** Thrown at /auth/refresh when a revoked token is presented again — indicates possible theft, so every active token for the user is revoked. */
public class RefreshTokenReusedException extends AuthException {

    /** Creates the exception with a fixed message. */
    public RefreshTokenReusedException() {
        super("REFRESH_TOKEN_REUSED", HttpStatus.UNAUTHORIZED,
                "A revoked refresh token was replayed; all sessions for this user have been revoked");
    }
}
