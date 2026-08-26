package com.readora.user.exception;

import org.springframework.http.HttpStatus;

public class CouponNotFoundException extends ServiceException {
    public CouponNotFoundException() {
        super("COUPON_NOT_FOUND", HttpStatus.NOT_FOUND, "No such coupon code");
    }
}
