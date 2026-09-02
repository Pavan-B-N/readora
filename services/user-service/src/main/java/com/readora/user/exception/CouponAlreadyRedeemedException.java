package com.readora.user.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

public class CouponAlreadyRedeemedException extends ServiceException {
    public CouponAlreadyRedeemedException() {
        super("COUPON_ALREADY_REDEEMED", HttpStatus.CONFLICT, "You've already redeemed this coupon");
    }
}
