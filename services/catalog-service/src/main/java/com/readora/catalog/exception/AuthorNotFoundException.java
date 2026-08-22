package com.readora.catalog.exception;

import org.springframework.http.HttpStatus;

public class AuthorNotFoundException extends ServiceException {
    public AuthorNotFoundException() {
        super("AUTHOR_NOT_FOUND", HttpStatus.NOT_FOUND, "No such author");
    }
}
