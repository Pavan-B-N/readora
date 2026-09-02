package com.readora.delivery.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

public class ReturnPickupNotFoundException extends ServiceException {
    public ReturnPickupNotFoundException() {
        super("RETURN_PICKUP_NOT_FOUND", HttpStatus.NOT_FOUND, "No such return pickup");
    }
}
