package com.readora.commerce.exception;

import org.springframework.http.HttpStatus;

public class VirtualEditionAlreadyOwnedException extends ServiceException {
    public VirtualEditionAlreadyOwnedException() {
        super("VIRTUAL_EDITION_ALREADY_OWNED", HttpStatus.CONFLICT, "You already own this book's virtual edition — open it from your library instead");
    }
}
