package com.readora.commerce.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

// Thrown when a referenced book doesn't exist or is inactive.
public class BookNotFoundException extends ServiceException {
    // Builds the 404 NOT_FOUND response.
    public BookNotFoundException() {
        super("BOOK_NOT_FOUND", HttpStatus.NOT_FOUND, "The book does not exist or is no longer active");
    }
}
