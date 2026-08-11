package com.readora.ai.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

// Thrown when a book hasn't been indexed for the assistant yet.
public class BookNotIndexedException extends ServiceException {
    // Builds the conflict response for an unindexed book.
    public BookNotIndexedException() {
        super("BOOK_NOT_INDEXED", HttpStatus.CONFLICT, "This book hasn't been prepared for the assistant yet — initialize it first");
    }
}
