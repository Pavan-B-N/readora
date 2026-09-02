package com.readora.catalog.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

public class VirtualEditionNotFoundException extends ServiceException {
    public VirtualEditionNotFoundException() {
        super("VIRTUAL_EDITION_NOT_FOUND", HttpStatus.NOT_FOUND, "No active virtual edition for this book");
    }
}
