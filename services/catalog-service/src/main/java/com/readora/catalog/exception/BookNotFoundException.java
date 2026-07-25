package com.readora.catalog.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

// Thrown when no active book exists with the given id.
public class BookNotFoundException extends ServiceException {
    public BookNotFoundException() {
        super("BOOK_NOT_FOUND", HttpStatus.NOT_FOUND, "No active book with that id");
    }
}
