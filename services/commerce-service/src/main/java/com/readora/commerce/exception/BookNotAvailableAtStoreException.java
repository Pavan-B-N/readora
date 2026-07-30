package com.readora.commerce.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

// Thrown when a requested book isn't stocked at the caller's delivery store.
public class BookNotAvailableAtStoreException extends ServiceException {
    // Builds the 409 CONFLICT response.
    public BookNotAvailableAtStoreException() {
        super("BOOK_NOT_AVAILABLE_AT_STORE", HttpStatus.CONFLICT,
                "This book isn't stocked at your delivery store");
    }
}
