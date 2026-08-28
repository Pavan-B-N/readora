package com.readora.delivery.exception;

import org.springframework.http.HttpStatus;

/** Two agents raced to claim the same return pickup — whoever loses gets this. */
public class ReturnPickupAlreadyClaimedException extends ServiceException {
    public ReturnPickupAlreadyClaimedException() {
        super("RETURN_PICKUP_ALREADY_CLAIMED", HttpStatus.CONFLICT, "Another agent already claimed this pickup");
    }
}
