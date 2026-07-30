package com.readora.commerce.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

// Thrown when checkout requests a virtual edition that doesn't exist for a book.
public class VirtualEditionNotAvailableException extends ServiceException {
    // Builds the 409 CONFLICT response naming the book.
    public VirtualEditionNotAvailableException(UUID bookId) {
        super("VIRTUAL_EDITION_NOT_AVAILABLE", HttpStatus.CONFLICT, "No virtual edition is available for book " + bookId);
    }
}
