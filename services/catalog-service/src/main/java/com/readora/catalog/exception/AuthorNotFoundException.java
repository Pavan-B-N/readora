package com.readora.catalog.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

public class AuthorNotFoundException extends ServiceException {
    public AuthorNotFoundException() {
        super("AUTHOR_NOT_FOUND", HttpStatus.NOT_FOUND, "No such author");
    }
}
