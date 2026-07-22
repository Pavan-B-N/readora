package com.readora.user.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

// Thrown when a coupon code doesn't exist.
public class CouponNotFoundException extends ServiceException {
    // 404 not found — no such coupon code.
    public CouponNotFoundException() {
        super("COUPON_NOT_FOUND", HttpStatus.NOT_FOUND, "No such coupon code");
    }
}
