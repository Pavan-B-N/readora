package com.readora.delivery.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

// No return pickup assignment exists for the given lookup.
public class ReturnPickupNotFoundException extends ServiceException {
    // Builds the 404 response for a missing return pickup.
    public ReturnPickupNotFoundException() {
        super("RETURN_PICKUP_NOT_FOUND", HttpStatus.NOT_FOUND, "No such return pickup");
    }
}
