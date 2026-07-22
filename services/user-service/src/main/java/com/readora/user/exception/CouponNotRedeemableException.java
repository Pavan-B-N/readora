package com.readora.user.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

// Thrown when a coupon exists but is expired or inactive.
public class CouponNotRedeemableException extends ServiceException {
    // 409 conflict — coupon expired or inactive.
    public CouponNotRedeemableException() {
        super("COUPON_NOT_REDEEMABLE", HttpStatus.CONFLICT, "This coupon has expired or is no longer active");
    }
}
