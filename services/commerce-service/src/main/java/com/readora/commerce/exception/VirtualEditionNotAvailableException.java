package com.readora.commerce.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class VirtualEditionNotAvailableException extends ServiceException {
    public VirtualEditionNotAvailableException(UUID bookId) {
        super("VIRTUAL_EDITION_NOT_AVAILABLE", HttpStatus.CONFLICT, "No virtual edition is available for book " + bookId);
    }
}
