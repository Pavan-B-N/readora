package com.readora.commerce.exception;

import org.springframework.http.HttpStatus;

public class ShippingAddressCityMismatchException extends ServiceException {
    public ShippingAddressCityMismatchException(String storeCity) {
        super(
                "SHIPPING_ADDRESS_CITY_MISMATCH",
                HttpStatus.BAD_REQUEST,
                "The shipping address must be in " + storeCity + " to deliver from this store"
        );
    }
}
