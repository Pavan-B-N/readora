package com.readora.user.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

public class CouponNotRedeemableException extends ServiceException {
    public CouponNotRedeemableException() {
        super("COUPON_NOT_REDEEMABLE", HttpStatus.CONFLICT, "This coupon has expired or is no longer active");
    }
}
