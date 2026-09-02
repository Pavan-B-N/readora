package com.readora.user.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

public class AddressNotFoundException extends ServiceException {
    public AddressNotFoundException() {
        super("ADDRESS_NOT_FOUND", HttpStatus.NOT_FOUND, "No such address belonging to the caller");
    }
}
