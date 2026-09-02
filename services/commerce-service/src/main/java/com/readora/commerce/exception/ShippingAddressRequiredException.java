package com.readora.commerce.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

public class ShippingAddressRequiredException extends ServiceException {
    public ShippingAddressRequiredException() {
        super("VALIDATION_FAILED", HttpStatus.BAD_REQUEST, "shippingAddress is required for a PHYSICAL order");
    }
}
