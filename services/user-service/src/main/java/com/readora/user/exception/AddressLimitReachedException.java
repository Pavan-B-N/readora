package com.readora.user.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

public class AddressLimitReachedException extends ServiceException {
    public AddressLimitReachedException() {
        super("ADDRESS_LIMIT_REACHED", HttpStatus.CONFLICT, "The account already holds the maximum of 20 addresses");
    }
}
