package com.readora.commerce.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

// Thrown when a PHYSICAL checkout is missing a shipping address.
public class ShippingAddressRequiredException extends ServiceException {
    // Builds the 400 BAD_REQUEST response.
    public ShippingAddressRequiredException() {
        super("VALIDATION_FAILED", HttpStatus.BAD_REQUEST, "shippingAddress is required for a PHYSICAL order");
    }
}
