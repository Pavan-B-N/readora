package com.readora.auth.exception;

import org.springframework.http.HttpStatus;

/** Base type for every domain exception this service raises; carries the error code and HTTP status GlobalExceptionHandler needs to build a response. */
public class AuthException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus status;

    /**
     * Creates a new domain exception.
     *
     * @param errorCode the stable machine-readable error code to return to the caller
     * @param status    the HTTP status to respond with
     * @param message   a human-readable description of what went wrong
     */
    public AuthException(String errorCode, HttpStatus status, String message) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    /** @return the stable machine-readable error code for this exception */
    public String getErrorCode() {
        return errorCode;
    }

    /** @return the HTTP status this exception should be reported as */
    public HttpStatus getStatus() {
        return status;
    }
}
