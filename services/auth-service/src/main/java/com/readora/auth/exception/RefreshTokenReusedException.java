package com.readora.auth.exception;

import org.springframework.http.HttpStatus;

public class RefreshTokenReusedException extends AuthException {

    public RefreshTokenReusedException() {
        super("REFRESH_TOKEN_REUSED", HttpStatus.UNAUTHORIZED,
                "A revoked refresh token was replayed; all sessions for this user have been revoked");
    }
}
