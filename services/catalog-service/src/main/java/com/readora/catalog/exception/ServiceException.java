package com.readora.catalog.exception;

import org.springframework.http.HttpStatus;

public class ServiceException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus status;

    public ServiceException(String errorCode, HttpStatus status, String message) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
