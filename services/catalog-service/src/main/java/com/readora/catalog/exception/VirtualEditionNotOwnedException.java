package com.readora.catalog.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

public class VirtualEditionNotOwnedException extends ServiceException {
    public VirtualEditionNotOwnedException() {
        super("VIRTUAL_EDITION_NOT_OWNED", HttpStatus.FORBIDDEN, "You haven't purchased this book");
    }
}
