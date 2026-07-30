package com.readora.commerce.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

// Thrown when checkout requests a virtual edition the caller already owns.
public class VirtualEditionAlreadyOwnedException extends ServiceException {
    // Builds the 409 CONFLICT response.
    public VirtualEditionAlreadyOwnedException() {
        super("VIRTUAL_EDITION_ALREADY_OWNED", HttpStatus.CONFLICT, "You already own this book's virtual edition — open it from your library instead");
    }
}
