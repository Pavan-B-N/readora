package com.readora.commerce.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

// Thrown when the shipping address's city doesn't match the fulfilling store's city.
public class ShippingAddressCityMismatchException extends ServiceException {
    // Builds the 400 BAD_REQUEST response naming the required city.
    public ShippingAddressCityMismatchException(String storeCity) {
        super(
                "SHIPPING_ADDRESS_CITY_MISMATCH",
                HttpStatus.BAD_REQUEST,
                "The shipping address must be in " + storeCity + " to deliver from this store"
        );
    }
}
