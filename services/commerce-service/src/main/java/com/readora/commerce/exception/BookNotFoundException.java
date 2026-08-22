package com.readora.commerce.exception;

import org.springframework.http.HttpStatus;

public class BookNotFoundException extends ServiceException {
    public BookNotFoundException() {
        super("BOOK_NOT_FOUND", HttpStatus.NOT_FOUND, "The book does not exist or is no longer active");
    }
}
