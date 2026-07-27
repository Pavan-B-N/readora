package com.readora.catalog.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

// Thrown when no active virtual edition exists for a book.
public class VirtualEditionNotFoundException extends ServiceException {
    public VirtualEditionNotFoundException() {
        super("VIRTUAL_EDITION_NOT_FOUND", HttpStatus.NOT_FOUND, "No active virtual edition for this book");
    }
}
