package com.readora.user.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

// Thrown when a user tries to redeem a coupon they already redeemed.
public class CouponAlreadyRedeemedException extends ServiceException {
    // 409 conflict — coupon already redeemed by this user.
    public CouponAlreadyRedeemedException() {
        super("COUPON_ALREADY_REDEEMED", HttpStatus.CONFLICT, "You've already redeemed this coupon");
    }
}
