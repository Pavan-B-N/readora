package com.readora.commerce.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

public class BookNotAvailableAtStoreException extends ServiceException {
    public BookNotAvailableAtStoreException() {
        super("BOOK_NOT_AVAILABLE_AT_STORE", HttpStatus.CONFLICT,
                "This book isn't stocked at your delivery store");
    }
}
