package com.readora.user.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

// Thrown when the caller references an address that doesn't exist or isn't theirs.
public class AddressNotFoundException extends ServiceException {
    // 404 not found — no such address for this caller.
    public AddressNotFoundException() {
        super("ADDRESS_NOT_FOUND", HttpStatus.NOT_FOUND, "No such address belonging to the caller");
    }
}
