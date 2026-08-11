package com.readora.ai.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

// Thrown when a user tries to access a book they haven't purchased.
public class BookAccessDeniedException extends ServiceException {
    // Builds the forbidden response for an unpurchased book.
    public BookAccessDeniedException() {
        super("BOOK_ACCESS_DENIED", HttpStatus.FORBIDDEN, "You haven't purchased this book");
    }
}
