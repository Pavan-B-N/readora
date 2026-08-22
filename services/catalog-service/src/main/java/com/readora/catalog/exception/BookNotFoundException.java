package com.readora.catalog.exception;

import org.springframework.http.HttpStatus;

public class BookNotFoundException extends ServiceException {
    public BookNotFoundException() {
        super("BOOK_NOT_FOUND", HttpStatus.NOT_FOUND, "No active book with that id");
    }
}
