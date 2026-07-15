package com.readora.sharedcore.exception;

import org.springframework.http.HttpStatus;

// A deliberately thrown, already-classified business error — carries the HTTP status and error code GlobalExceptionHandler serializes back to the caller.
public class ServiceException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus status;

    // Builds the exception with the code/status/message GlobalExceptionHandler will read back out.
    public ServiceException(String errorCode, HttpStatus status, String message) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    // The machine-readable error code (e.g. "CATALOG_SERVICE_UNAVAILABLE").
    public String getErrorCode() {
        return errorCode;
    }

    // The HTTP status this error should be returned as.
    public HttpStatus getStatus() {
        return status;
    }
}
