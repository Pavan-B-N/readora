package com.readora.user.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

// Thrown when a user tries to add an address beyond the 20-address limit.
public class AddressLimitReachedException extends ServiceException {
    // 409 conflict — address limit reached.
    public AddressLimitReachedException() {
        super("ADDRESS_LIMIT_REACHED", HttpStatus.CONFLICT, "The account already holds the maximum of 20 addresses");
    }
}
