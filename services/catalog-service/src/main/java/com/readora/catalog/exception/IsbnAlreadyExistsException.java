package com.readora.catalog.exception;

import org.springframework.http.HttpStatus;

/** Thrown when creating a book whose ISBN-13 already belongs to another book. */
public class IsbnAlreadyExistsException extends ServiceException {
    public IsbnAlreadyExistsException(String isbn13) {
        super(
                "ISBN_ALREADY_EXISTS",
                HttpStatus.CONFLICT,
                "A book with ISBN " + isbn13 + " already exists — increase its stock instead of creating a duplicate"
        );
    }
}
