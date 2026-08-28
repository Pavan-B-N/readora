package com.readora.catalog.exception;

import org.springframework.http.HttpStatus;

public class AuthorInUseException extends ServiceException {
    public AuthorInUseException() {
        super("AUTHOR_IN_USE", HttpStatus.CONFLICT, "This author is credited on one or more books — remove them from those books first");
    }
}
